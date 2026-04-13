package com.seckill.service;

import com.seckill.config.MonitorProperties;
import com.seckill.entity.ExceptionLog;
import com.seckill.mapper.ExceptionLogMapper;
import com.seckill.mapper.SeckillEventMapper;
import com.seckill.entity.SeckillEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 秒杀监控与告警：在内存中按「分钟桶」统计请求量、成功、失败、限流等；定时任务里算 QPS、比对 Redis/DB 库存；
 * 异常信息写入 exception_log 表，供后台看板查询。
 */
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final ExceptionLogMapper exceptionLogMapper;
    private final MonitorProperties monitorProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillEventMapper seckillEventMapper;

    /** 键为「年-月-日-时-分」字符串，值为该分钟内的计数桶；并发安全。 */
    private final ConcurrentHashMap<String, MinuteBucket> buckets = new ConcurrentHashMap<>();
    /** 连续 Lua/Redis 执行失败次数，超过配置阈值则落库告警并重置。 */
    private final AtomicInteger consecutiveLuaErrors = new AtomicInteger(0);

    /** 秒杀请求进入（含最终失败），用于估算流量。 */
    public void recordSeckillRequest() {
        currentBucket().total.incrementAndGet();
    }

    /** Lua 与 DB 均成功落单。 */
    public void recordSeckillSuccess() {
        currentBucket().success.incrementAndGet();
    }

    /** 库存不足、路径无效、时间不对等「未成功抢到」类情况。 */
    public void recordStockShort() {
        currentBucket().stockShort.incrementAndGet();
    }

    /** 被 {@link com.seckill.aspect.RateLimitAspect} 拦截。 */
    public void recordRateLimited() {
        currentBucket().rateLimited.incrementAndGet();
    }

    /** 业务异常或其它未分类错误（可按需细化）。 */
    public void recordException() {
        currentBucket().errors.incrementAndGet();
    }

    /**
     * Redis 执行 Lua 抛错时 redisThrew=true，累计连续次数；成功执行一次则清零连续计数。
     */
    public void recordLuaScriptError(boolean redisThrew) {
        if (redisThrew) {
            int n = consecutiveLuaErrors.incrementAndGet();
            if (n >= monitorProperties.getLuaErrorConsecutive()) {
                saveLog("LUA_ERROR", "连续 Redis/Lua 执行异常", "count=" + n);
                consecutiveLuaErrors.set(0);
            }
        } else {
            consecutiveLuaErrors.set(0);
        }
    }

    /**
     * 由定时任务每分钟调用：取上一分钟桶内总请求数，除以 60 得近似 QPS；超阈值写日志。
     */
    public void evaluateMinuteQps() {
        String key = minuteKey();
        MinuteBucket b = buckets.get(key);
        if (b == null) {
            return;
        }
        long total = b.total.get();
        double qps = total / 60.0;
        if (qps > monitorProperties.getHighQpsThreshold()) {
            saveLog("HIGH_QPS", "秒杀接口 QPS 超阈值", "approxQps=" + qps + ", total=" + total);
        }
    }

    /**
     * 遍历启用中的秒杀场次，比对 Redis 剩余库存与 MySQL 字段差值；偏差过大写告警（运维或数据修复线索）。
     */
    public void checkStockMismatch() {
        List<SeckillEvent> list = seckillEventMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<SeckillEvent>lambdaQuery()
                        .eq(SeckillEvent::getStatus, 1));
        for (SeckillEvent e : list) {
            if (e.getEndTime().isBefore(LocalDateTime.now())) {
                continue;
            }
            String redisStock = stringRedisTemplate.opsForValue().get("seckill:stock:" + e.getId());
            if (redisStock == null) {
                continue;
            }
            int r = Integer.parseInt(redisStock);
            int diff = Math.abs(r - e.getSeckillStock());
            if (diff > monitorProperties.getStockMismatchThreshold()) {
                saveLog("STOCK_MISMATCH", "Redis 与 MySQL 秒杀库存差异过大",
                        "itemId=" + e.getId() + ", redis=" + r + ", mysql=" + e.getSeckillStock());
            }
        }
    }

    /**
     * 管理端看板：返回最近 60 个「整分」的近似 QPS 与库存差异采样（分钟桶内 lastMismatch）。
     */
    public List<Map<String, Object>> lastHourStats() {
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 59; i >= 0; i--) {
            LocalDateTime t = now.minusMinutes(i).withSecond(0).withNano(0);
            String k = t.getYear() + "-" + t.getMonthValue() + "-" + t.getDayOfMonth() + "-" + t.getHour() + "-" + t.getMinute();
            MinuteBucket b = buckets.get(k);
            double qps = b == null ? 0 : b.total.get() / 60.0;
            int mismatch = b == null ? 0 : b.lastMismatch.get();
            out.add(Map.of(
                    "minute", t.toString(),
                    "qps", Math.round(qps * 100.0) / 100.0,
                    "stockDiffSample", mismatch
            ));
        }
        return out;
    }

    /** 供对账逻辑把「当前分钟最后一次算出的差值」写入桶，便于曲线展示。 */
    public void setLastMismatchSample(int v) {
        currentBucket().lastMismatch.set(v);
    }

    /** 取当前分钟对应的桶，不存在则新建。 */
    private MinuteBucket currentBucket() {
        return buckets.computeIfAbsent(minuteKey(), k -> new MinuteBucket());
    }

    /** 与桶 key 规则一致：截断到分钟，避免同一分钟内秒级抖动。 */
    private static String minuteKey() {
        LocalDateTime n = LocalDateTime.now().withSecond(0).withNano(0);
        return n.getYear() + "-" + n.getMonthValue() + "-" + n.getDayOfMonth() + "-" + n.getHour() + "-" + n.getMinute();
    }

    private void saveLog(String type, String message, String detail) {
        ExceptionLog log = new ExceptionLog();
        log.setType(type);
        log.setMessage(message);
        log.setDetail(detail);
        log.setCreateTime(LocalDateTime.now());
        exceptionLogMapper.insert(log);
    }

    /** 单分钟内的原子计数器集合，用 Atomic* 支持高并发累加。 */
    private static class MinuteBucket {
        final AtomicLong total = new AtomicLong();
        final AtomicLong success = new AtomicLong();
        final AtomicLong stockShort = new AtomicLong();
        final AtomicLong rateLimited = new AtomicLong();
        final AtomicLong errors = new AtomicLong();
        final AtomicInteger lastMismatch = new AtomicInteger();
    }
}

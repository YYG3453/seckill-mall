package com.seckill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.entity.Product;
import com.seckill.entity.SeckillEvent;
import com.seckill.exception.BusinessException;
import com.seckill.exception.SeckillException;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.SeckillEventMapper;
import com.seckill.utils.PathGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 秒杀领域核心服务：Redis 与 MySQL 双写协同。
 * <p>
 * <b>Redis 侧</b>
 * <ul>
 *   <li>{@code seckill:event:{itemId}} Hash：场次元数据（价、起止时间、状态快照、productId）。</li>
 *   <li>{@code seckill:stock:{itemId}} String：可抢库存，由 Lua 原子 DECR，与 DB 乐观扣减配合。</li>
 *   <li>{@code seckill:user:item:{userId}:{itemId}}：Lua 内 SETNX+TTL，保证每人每场最多成功一次。</li>
 *   <li>{@code seckill:path:{userId}:{itemId}}：动态 path，防接口裸刷。</li>
 *   <li>{@code seckill:events} Set：活跃场次 ID 集合，供监控/预热遍历。</li>
 * </ul>
 * <b>流程摘要</b>：启动 {@link #warmup()} 把未结束场次灌入 Redis；下单时先校验时间与 path，再执行 Lua；
 * Lua 返回成功后 {@link SeckillTxService#placeSeckillOrder} 写订单并 DB 扣库存；若 DB 失败则 Java 回滚 Redis 库存与限购 key。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final SeckillEventMapper seckillEventMapper;
    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillLuaScript;
    private final SeckillTxService seckillTxService;

    /**
     * 应用启动时：将所有「后台标记为启用 status=1」且尚未结束的场次同步到 Redis，避免冷启动后库存 key 缺失。
     */
    @PostConstruct
    public void warmup() {
        List<SeckillEvent> list = seckillEventMapper.selectList(Wrappers.<SeckillEvent>lambdaQuery()
                .eq(SeckillEvent::getStatus, 1));
        LocalDateTime now = LocalDateTime.now();
        for (SeckillEvent e : list) {
            if (e.getEndTime().isAfter(now)) {
                syncToRedis(e);
            }
        }
        log.info("seckill redis warmup done, count={}", list.size());
    }

    /**
     * 单场次全量同步：写 Hash、注册 events 集合、初始化库存 String（与后台配置的 seckillStock 对齐）。
     * status 字段按当前时间相对 start/end 计算 0 未开始 / 1 进行中 / 2 已结束，供展示或运维查看。
     */
    public void syncToRedis(SeckillEvent e) {
        String itemId = String.valueOf(e.getId());
        LocalDateTime now = LocalDateTime.now();
        int status;
        if (now.isBefore(e.getStartTime())) {
            status = 0;
        } else if (now.isAfter(e.getEndTime())) {
            status = 2;
        } else {
            status = 1;
        }
        Map<String, String> hash = new HashMap<>();
        hash.put("seckillPrice", e.getSeckillPrice().toPlainString());
        hash.put("seckillStock", String.valueOf(e.getSeckillStock()));
        hash.put("startTime", e.getStartTime().toString());
        hash.put("endTime", e.getEndTime().toString());
        hash.put("status", String.valueOf(status));
        hash.put("productId", String.valueOf(e.getProductId()));
        stringRedisTemplate.opsForHash().putAll("seckill:event:" + itemId, hash);
        stringRedisTemplate.opsForSet().add("seckill:events", itemId);
        stringRedisTemplate.opsForValue().set("seckill:stock:" + itemId, String.valueOf(e.getSeckillStock()));
    }

    /** 场次删除或禁用时清理 Redis，防止前台仍看到旧库存。 */
    public void removeFromRedis(Long eventId) {
        String itemId = String.valueOf(eventId);
        stringRedisTemplate.delete("seckill:event:" + itemId);
        stringRedisTemplate.delete("seckill:stock:" + itemId);
        stringRedisTemplate.opsForSet().remove("seckill:events", itemId);
    }

    /** 按商品找最近一场未结束的有效秒杀，供商品详情聚合展示。 */
    public Map<String, Object> publicEventByProductId(Long productId) {
        SeckillEvent e = seckillEventMapper.selectOne(
                Wrappers.<SeckillEvent>lambdaQuery()
                        .eq(SeckillEvent::getProductId, productId)
                        .eq(SeckillEvent::getStatus, 1)
                        .gt(SeckillEvent::getEndTime, LocalDateTime.now())
                        .orderByAsc(SeckillEvent::getStartTime)
                        .last("limit 1"));
        return e == null ? null : publicEvent(e.getId());
    }

    /**
     * 公开接口数据组装：校验场次启用且未过结束时间；库存数字以 Redis 为准（{@link #currentRedisStock}），与真实可抢一致。
     */
    public Map<String, Object> publicEvent(Long itemId) {
        SeckillEvent e = seckillEventMapper.selectById(itemId);
        if (e == null || e.getStatus() == null || e.getStatus() != 1) {
            return null;
        }
        if (e.getEndTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("itemId", e.getId());
        m.put("seckillPrice", e.getSeckillPrice());
        m.put("seckillStock", currentRedisStock(itemId));
        m.put("startTime", e.getStartTime());
        m.put("endTime", e.getEndTime());
        m.put("productId", e.getProductId());
        return m;
    }

    /**
     * 读 {@code seckill:stock:{itemId}}；若 key 不存在（例如被手动删 key）则回退读 DB 配置库存，保证页面有数可显。
     */
    public int currentRedisStock(Long itemId) {
        String v = stringRedisTemplate.opsForValue().get("seckill:stock:" + itemId);
        if (v == null) {
            SeckillEvent e = seckillEventMapper.selectById(itemId);
            return e == null ? 0 : e.getSeckillStock();
        }
        return Integer.parseInt(v);
    }

    /**
     * 动态路径：随机 path 写入 Redis，仅持有合法 path 的请求才能进入 Lua 扣库存逻辑，避免接口被直接枚举。
     */
    public String generatePath(Long userId, Long itemId) {
        validateEventActive(itemId);
        String path = PathGenerator.randomPath();
        stringRedisTemplate.opsForValue().set(
                "seckill:path:" + userId + ":" + itemId,
                path,
                Duration.ofMinutes(5));
        return path;
    }

    /**
     * 秒杀主链路：监控埋点 → 校验场次/时间/动态 path/商品可售 → 执行 Lua（原子扣库存+写限购）→ 事务落单。
     * <p>
     * Lua 的 ARGV[1] 为限购 key 的 TTL（秒），取「距结束时间 + 1 天」与下限 60 秒，避免 key 永久残留。
     * 若 {@link SeckillTxService#placeSeckillOrder} 抛业务异常（如 DB 库存不足），则 INCR 回滚 Redis 库存并删除限购 key。
     */
    public String doSeckill(Long userId, String path, Long itemId, MonitorService monitor) {
        monitor.recordSeckillRequest();
        SeckillEvent ev = seckillEventMapper.selectById(itemId);
        if (ev == null || ev.getStatus() == null || ev.getStatus() != 1) {
            monitor.recordStockShort();
            throw new SeckillException("秒杀场次无效");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(ev.getStartTime()) || now.isAfter(ev.getEndTime())) {
            monitor.recordStockShort();
            throw new SeckillException("不在秒杀时间");
        }
        String expect = stringRedisTemplate.opsForValue().get("seckill:path:" + userId + ":" + itemId);
        if (expect == null || !expect.equals(path)) {
            monitor.recordStockShort();
            throw new SeckillException("秒杀路径无效或已过期");
        }

        Product product = productMapper.selectById(ev.getProductId());
        if (product == null || product.getStatus() != 1) {
            monitor.recordStockShort();
            throw new SeckillException("商品不可售");
        }

        long ttl = Duration.between(now, ev.getEndTime()).getSeconds() + 86400;
        if (ttl < 60) {
            ttl = 60;
        }
        String stockKey = "seckill:stock:" + itemId;
        String userKey = "seckill:user:item:" + userId + ":" + itemId;
        Long ok;
        try {
            ok = stringRedisTemplate.execute(
                    seckillLuaScript,
                    Arrays.asList(stockKey, userKey),
                    String.valueOf(ttl));
        } catch (Exception ex) {
            monitor.recordLuaScriptError(true);
            monitor.recordException();
            log.error("lua execute error userId={} itemId={}", userId, itemId, ex);
            throw new SeckillException("秒杀失败，请重试");
        }
        monitor.recordLuaScriptError(false);
        if (ok == null || ok == 0L) {
            monitor.recordStockShort();
            throw new SeckillException("秒杀失败，库存不足或已参与");
        }

        try {
            String orderNo = seckillTxService.placeSeckillOrder(userId, ev, product);
            monitor.recordSeckillSuccess();
            return orderNo;
        } catch (BusinessException ex) {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(userKey);
            monitor.recordStockShort();
            log.warn("seckill compensate userId={} itemId={} msg={}", userId, itemId, ex.getMessage());
            throw new SeckillException(ex.getMessage());
        }
    }

    /** 领取 path 前校验：场次存在、启用、且未过结束时间。 */
    private void validateEventActive(Long itemId) {
        SeckillEvent ev = seckillEventMapper.selectById(itemId);
        if (ev == null || ev.getStatus() == null || ev.getStatus() != 1) {
            throw new BusinessException("秒杀场次不存在");
        }
        if (ev.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("秒杀已结束");
        }
    }

    /** 返回 Redis Set {@code seckill:events} 中的场次 ID，供定时任务或监控使用。 */
    public Set<Long> activeEventIds() {
        Set<String> m = stringRedisTemplate.opsForSet().members("seckill:events");
        if (m == null) {
            return Set.of();
        }
        return m.stream().map(Long::parseLong).collect(Collectors.toSet());
    }
}

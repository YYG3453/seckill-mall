package com.seckill.task;

import com.seckill.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 监控定时任务：每分钟触发，与 {@link MonitorService} 配合，计算 QPS 并做 Redis/MySQL 秒杀库存对账。
 */
@Component
@RequiredArgsConstructor
public class MonitorTask {

    private final MonitorService monitorService;

    /**
     * 每分钟第 0 秒执行：根据上一分钟记录的请求次数估算 QPS，用于看板或日志。
     * cron 六位：秒 分 时 日 月 周；{@code 0 * * * * ?} 表示每分钟的 0 秒。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void qps() {
        monitorService.evaluateMinuteQps();
    }

    /**
     * 每分钟第 30 秒执行：与 qps 错开，避免同一时刻任务堆积；检查 Redis 秒杀库存与 DB 是否偏差过大。
     */
    @Scheduled(cron = "30 * * * * ?")
    public void stock() {
        monitorService.checkStockMismatch();
    }
}

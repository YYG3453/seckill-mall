package com.seckill.task;

import com.seckill.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时扫描：周期性调用 {@link OrderTimeoutService}，取消「创建超过 30 分钟仍未支付」的订单并归还库存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderTimeoutService orderTimeoutService;

    /**
     * 每分钟第 0 秒执行一次扫描；内部 catch 异常避免单次失败影响后续调度。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void run() {
        try {
            orderTimeoutService.cancelUnpaidOlderThan30Minutes();
        } catch (Exception e) {
            log.error("order timeout scan failed", e);
        }
    }
}

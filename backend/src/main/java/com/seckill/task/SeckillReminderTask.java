package com.seckill.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.entity.Product;
import com.seckill.entity.SeckillEvent;
import com.seckill.entity.SeckillReminder;
import com.seckill.entity.UserNotification;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.SeckillEventMapper;
import com.seckill.mapper.SeckillReminderMapper;
import com.seckill.mapper.UserNotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀开抢提醒：扫描「即将在 5 分钟内开始」的场次，给已预约提醒的用户写入站内通知，并标记提醒已处理。
 */
@Component
@RequiredArgsConstructor
public class SeckillReminderTask {

    private final SeckillReminderMapper seckillReminderMapper;
    private final SeckillEventMapper seckillEventMapper;
    private final ProductMapper productMapper;
    private final UserNotificationMapper userNotificationMapper;

    /**
     * 每 2 分钟执行一次（Spring cron：分字段为从 0 起每 2 分钟一步进，秒固定为 0）。
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        // 只关心「未来 5 分钟内即将开始」的启用场次，避免扫全表历史数据
        LocalDateTime until = now.plusMinutes(5);
        List<SeckillEvent> events = seckillEventMapper.selectList(
                Wrappers.<SeckillEvent>lambdaQuery()
                        .eq(SeckillEvent::getStatus, 1)
                        .gt(SeckillEvent::getStartTime, now)
                        .le(SeckillEvent::getStartTime, until));
        for (SeckillEvent ev : events) {
            // 该场次下仍待发送（status=0）的提醒记录
            List<SeckillReminder> rs = seckillReminderMapper.selectList(
                    Wrappers.<SeckillReminder>lambdaQuery()
                            .eq(SeckillReminder::getItemId, ev.getId())
                            .eq(SeckillReminder::getStatus, 0));
            Product p = productMapper.selectById(ev.getProductId());
            String name = p == null ? "商品" : p.getName();
            for (SeckillReminder r : rs) {
                UserNotification n = new UserNotification();
                n.setUserId(r.getUserId());
                n.setContent("您关注的【" + name + "】秒杀即将开始！");
                n.setIsRead(0);
                n.setCreateTime(LocalDateTime.now());
                userNotificationMapper.insert(n);
                r.setStatus(1);
                seckillReminderMapper.updateById(r);
            }
        }
    }
}

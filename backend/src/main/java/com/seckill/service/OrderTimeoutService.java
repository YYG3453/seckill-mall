package com.seckill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.entity.OrderEntity;
import com.seckill.entity.OrderItem;
import com.seckill.mapper.OrderItemMapper;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.SeckillEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时关单：扫描创建时间早于「当前-30 分钟」且状态仍为「待支付」的订单，逐单归还库存。
 * <p>
 * 秒杀单行：同时加回 MySQL 场次库存、Redis {@code seckill:stock}，并删除限购 key {@code seckill:user:item:{userId}:{eventId}}，使用户可再次抢购。
 * 普通行：按数量加回商品表库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimeoutService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final SeckillEventMapper seckillEventMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 由定时任务调用；一单内多行 item 循环处理，最后将订单状态置为「已取消」。 */
    @Transactional(rollbackFor = Exception.class)
    public void cancelUnpaidOlderThan30Minutes() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);
        List<OrderEntity> list = orderMapper.selectList(Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getStatus, "待支付")
                .lt(OrderEntity::getCreateTime, deadline));
        for (OrderEntity o : list) {
            List<OrderItem> items = orderItemMapper.selectList(
                    Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, o.getId()));
            for (OrderItem oi : items) {
                if (oi.getSeckillFlag() != null && oi.getSeckillFlag() == 1 && oi.getSeckillEventId() != null) {
                    seckillEventMapper.increaseStockOne(oi.getSeckillEventId());
                    stringRedisTemplate.opsForValue().increment("seckill:stock:" + oi.getSeckillEventId());
                    stringRedisTemplate.delete("seckill:user:item:" + o.getUserId() + ":" + oi.getSeckillEventId());
                } else {
                    productMapper.increaseStock(oi.getProductId(), oi.getQuantity());
                }
            }
            o.setStatus("已取消");
            orderMapper.updateById(o);
            log.info("order timeout cancelled orderNo={} userId={}", o.getOrderNo(), o.getUserId());
        }
    }
}

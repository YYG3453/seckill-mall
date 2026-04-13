package com.seckill.service;

import com.seckill.entity.OrderEntity;
import com.seckill.entity.OrderItem;
import com.seckill.entity.Product;
import com.seckill.entity.SeckillEvent;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.OrderItemMapper;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.SeckillEventMapper;
import com.seckill.utils.OrderNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀下单的事务边界：在单事务内插入订单主表、订单明细，并对秒杀场次表做「条件更新」扣减 DB 库存。
 * <p>
 * <b>为何在 Lua 之后还要动 DB？</b> Redis 承担高并发下的原子扣减与防重；MySQL 仍是权威持久化与对账依据。
 * 若仅信 Redis，重启或主从延迟可能导致与财务/库存报表不一致，故用 {@code decreaseStockOne}（典型实现为
 * {@code UPDATE ... SET seckill_stock = seckill_stock - 1 WHERE id=? AND seckill_stock >= 1}）做最后一道闸。
 * <p>
 * <b>失败时的补偿</b>：由调用方 {@link com.seckill.service.SeckillService#doSeckill} 捕获 {@link BusinessException}，
 * 对 Redis 执行 INCR 库存、DEL 限购 key，使用户可再次尝试，且库存数字与限购状态回到 Lua 执行前一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillTxService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SeckillEventMapper seckillEventMapper;

    /**
     * 生成订单号 → 写订单（待支付）→ 写单行明细（带秒杀标记与秒杀价）→ DB 扣减场次库存一行。
     *
     * @return 订单号供前端跳转支付或订单列表查询
     */
    @Transactional(rollbackFor = Exception.class)
    public String placeSeckillOrder(Long userId, SeckillEvent ev, Product product) {
        String orderNo = OrderNoGenerator.next();
        OrderEntity order = new OrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(ev.getSeckillPrice());
        order.setStatus("待支付");
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        OrderItem oi = new OrderItem();
        oi.setOrderId(order.getId());
        oi.setProductId(product.getId());
        oi.setSeckillEventId(ev.getId());
        oi.setSeckillFlag(1);
        oi.setSeckillPrice(ev.getSeckillPrice());
        oi.setUnitPrice(ev.getSeckillPrice());
        oi.setQuantity(1);
        orderItemMapper.insert(oi);

        int rows = seckillEventMapper.decreaseStockOne(ev.getId());
        if (rows == 0) {
            throw new BusinessException("秒杀失败，库存不足");
        }
        log.info("seckill order placed userId={} itemId={} orderNo={}", userId, ev.getId(), orderNo);
        return orderNo;
    }
}

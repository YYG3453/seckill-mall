package com.seckill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.dto.CartLine;
import com.seckill.entity.OrderEntity;
import com.seckill.entity.OrderItem;
import com.seckill.entity.Product;
import com.seckill.entity.UserActionLog;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.OrderItemMapper;
import com.seckill.mapper.OrderMapper;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.UserActionLogMapper;
import com.seckill.utils.OrderNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 普通商城订单：从购物车勾选行生成一张「待支付」订单，按行扣减 {@link Product} 表库存，并写入行为日志（推荐系统用）。
 * <p>
 * 与秒杀区别：不走 Redis Lua，直接依赖 Mapper 层原子扣减（如 {@code stock >= qty} 条件更新）；支付为演示用的「模拟支付」；
 * 超时未支付由 {@link OrderTimeoutService} 定时取消并归还库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final CartService cartService;
    private final UserActionLogMapper userActionLogMapper;

    /**
     * 读取 Redis 购物车 Hash，仅处理 {@code selected=true} 且数量&gt;0 的行；校验商品上架与库存后汇总金额下单。
     * 任一行扣减失败则整单回滚（事务）；成功后从购物车移除已结算商品，并记录一条 {@code buy} 行为日志。
     */
    @Transactional(rollbackFor = Exception.class)
    public String createFromCart(Long userId) {
        Map<Long, CartLine> cart = cartService.list(userId);
        List<Line> selected = new ArrayList<>();
        for (Map.Entry<Long, CartLine> e : cart.entrySet()) {
            CartLine line = e.getValue();
            if (line.isSelected() && line.getQuantity() > 0) {
                Product p = productMapper.selectById(e.getKey());
                if (p == null || p.getStatus() == null || p.getStatus() != 1) {
                    throw new BusinessException("商品不可购买: " + e.getKey());
                }
                if (p.getStock() < line.getQuantity()) {
                    throw new BusinessException("库存不足: " + p.getName());
                }
                selected.add(new Line(p, line.getQuantity()));
            }
        }
        if (selected.isEmpty()) {
            throw new BusinessException("请选择结算商品");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Line l : selected) {
            total = total.add(l.product.getPrice().multiply(BigDecimal.valueOf(l.qty)));
        }
        String orderNo = OrderNoGenerator.next();
        OrderEntity order = new OrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus("待支付");
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        for (Line l : selected) {
            int rows = productMapper.decreaseStock(l.product.getId(), l.qty);
            if (rows == 0) {
                throw new BusinessException("库存不足: " + l.product.getName());
            }
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(l.product.getId());
            oi.setSeckillFlag(0);
            oi.setSeckillPrice(null);
            oi.setUnitPrice(l.product.getPrice());
            oi.setQuantity(l.qty);
            orderItemMapper.insert(oi);
        }
        List<Long> pids = new ArrayList<>();
        for (Line l : selected) {
            pids.add(l.product.getId());
        }
        cartService.removeKeys(userId, pids);

        UserActionLog al = new UserActionLog();
        al.setUserId(userId);
        al.setItemId(selected.get(0).product.getId());
        al.setActionType("buy");
        al.setCreateTime(LocalDateTime.now());
        userActionLogMapper.insert(al);

        log.info("order created userId={} orderNo={}", userId, orderNo);
        return orderNo;
    }

    /** 演示环境：将「待支付」改为「已支付」并写入支付时间；后台可再「发货」。 */
    @Transactional(rollbackFor = Exception.class)
    public void simulatePay(Long userId, String orderNo) {
        OrderEntity o = orderMapper.selectOne(Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getOrderNo, orderNo));
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (!"待支付".equals(o.getStatus())) {
            throw new BusinessException("订单状态不可支付");
        }
        o.setStatus("已支付");
        o.setPayTime(LocalDateTime.now());
        orderMapper.updateById(o);
        log.info("order pay userId={} orderNo={}", userId, orderNo);
    }

    /** 当前用户订单列表，按创建时间倒序。 */
    public List<OrderEntity> listByUser(Long userId) {
        return orderMapper.selectList(Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getUserId, userId)
                .orderByDesc(OrderEntity::getCreateTime));
    }

    /** 防越权：订单必须属于该 userId。 */
    public OrderEntity detailByUser(Long userId, Long orderId) {
        OrderEntity o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return o;
    }

    /** 订单明细列表（含秒杀标记时用于超时任务区分归还逻辑）。 */
    public List<OrderItem> items(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId));
    }

    /** 管理端分页订单列表。 */
    public List<OrderEntity> listAll(int page, int size) {
        return orderMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                Wrappers.<OrderEntity>lambdaQuery().orderByDesc(OrderEntity::getCreateTime)
        ).getRecords();
    }

    /** 仅「已支付」可发货，状态变为「已发货」。 */
    @Transactional(rollbackFor = Exception.class)
    public void ship(String orderNo) {
        OrderEntity o = orderMapper.selectOne(Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getOrderNo, orderNo));
        if (o == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"已支付".equals(o.getStatus())) {
            throw new BusinessException("仅已支付订单可发货");
        }
        o.setStatus("已发货");
        orderMapper.updateById(o);
    }

    private static class Line {
        final Product product;
        final int qty;

        Line(Product product, int qty) {
            this.product = product;
            this.qty = qty;
        }
    }
}

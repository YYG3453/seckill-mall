package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细实体（对应 order_item 表）。
 * 功能：记录订单中的商品行、单价、数量，以及是否秒杀与秒杀场次关联。
 * 创建原因：支持一单多商品、秒杀与普通单并存，并为超时回滚库存提供精确粒度。
 */
@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    /** 秒杀场次 ID，用于超时取消时回滚 Redis/MySQL 秒杀库存 */
    private Long seckillEventId;
    private Integer seckillFlag;
    private BigDecimal seckillPrice;
    private BigDecimal unitPrice;
    private Integer quantity;
}

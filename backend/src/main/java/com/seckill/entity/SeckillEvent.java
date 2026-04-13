package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀场次实体（对应 seckill_event 表）。
 * 功能：定义某商品的秒杀价格、库存、开始结束时间与启用状态。
 * 创建原因：将秒杀配置独立于商品主表，支持同商品在不同时间段多场活动运营。
 */
@Data
@TableName("seckill_event")
public class SeckillEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}

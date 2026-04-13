package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体（对应 `order` 表）。
 * 功能：记录订单号、用户、总金额、状态与支付时间等主信息。
 * 创建原因：将订单主数据与订单明细拆分建模，便于状态流转与统计分析。
 */
@Data
@TableName("`order`")
public class OrderEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
}

package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（对应 product 表）。
 * 功能：承载商品基础信息、价格、库存、上下架状态等字段。
 * 创建原因：作为 MyBatis-Plus 的持久化对象，在 mapper/service/controller 间统一传递商品数据。
 */
@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer categoryId;
    private BigDecimal price;
    private Integer stock;
    private String description;
    private String image;
    private Integer status;
    private String tag;
    private LocalDateTime createTime;
}

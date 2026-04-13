package com.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 购物车单行 DTO。
 * 功能：描述某商品在购物车中的数量与是否勾选。
 * 创建原因：把 Redis 购物车 value 结构固定下来，便于序列化/反序列化与结算逻辑复用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartLine {
    private int quantity;
    private boolean selected;
}

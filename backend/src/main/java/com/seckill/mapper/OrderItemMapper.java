package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper。
 * 功能：提供 OrderItem 的增删改查，供订单详情展示与库存回滚逻辑读取明细行。
 * 创建原因：订单主表与明细表拆分后，需要独立的数据访问层承载一对多关系查询。
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}

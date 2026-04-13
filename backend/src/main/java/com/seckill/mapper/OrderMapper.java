package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单主表 Mapper。
 * 功能：提供 OrderEntity 的增删改查与分页查询能力。
 * 创建原因：作为订单主数据访问入口，供下单、支付、后台管理和统计服务复用。
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
}

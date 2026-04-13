package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.SeckillEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀场次 Mapper。
 * 功能：提供 SeckillEvent CRUD 以及秒杀库存 +1/-1 的原子更新语句。
 * 创建原因：秒杀库存扣减必须在数据库层具备条件更新能力，防止并发超卖。
 */
@Mapper
public interface SeckillEventMapper extends BaseMapper<SeckillEvent> {

    @Update("UPDATE seckill_event SET seckill_stock = seckill_stock - 1 WHERE id = #{id} AND seckill_stock >= 1")
    int decreaseStockOne(@Param("id") Long id);

    @Update("UPDATE seckill_event SET seckill_stock = seckill_stock + 1 WHERE id = #{id}")
    int increaseStockOne(@Param("id") Long id);
}

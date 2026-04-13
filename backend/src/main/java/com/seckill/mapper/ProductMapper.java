package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品表 Mapper。
 * 功能：提供 Product 基础 CRUD 与库存原子增减 SQL。
 * 创建原因：把数据库访问与业务逻辑分层，库存扣减可在 SQL 层保证并发安全条件。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("UPDATE product SET stock = stock - #{qty} WHERE id = #{id} AND stock >= #{qty}")
    int decreaseStock(@Param("id") Long id, @Param("qty") int qty);

    @Update("UPDATE product SET stock = stock + #{qty} WHERE id = #{id}")
    int increaseStock(@Param("id") Long id, @Param("qty") int qty);
}

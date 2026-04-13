package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类表 Mapper。
 * 功能：提供 Category 的基础 CRUD，供前台分类展示与后台分类维护使用。
 * 创建原因：把分类数据访问独立出来，减少控制器/服务层直接耦合 SQL。
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}

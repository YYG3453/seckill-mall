package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 分类实体（对应 category 表）。
 * 功能：维护商品分类树节点（id、name、parentId）。
 * 创建原因：把分类作为独立主数据，便于前台筛选与后台分类管理。
 */
@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer parentId;
}

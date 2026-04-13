package com.seckill.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.dto.ApiResult;
import com.seckill.entity.Category;
import com.seckill.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类只读列表：供首页侧栏、搜索筛选等使用；无需登录，路径在拦截器排除列表中。
 * 前端调用关系：由 {@code frontend/src/api/product.js} 的 {@code fetchCategories()} 调用，
 * 主要被 Home.vue、Search.vue、ProductDetail.vue 使用。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryMapper categoryMapper;

    /** 全部分类列表，无分页；前台侧栏与筛选使用。 */
    @GetMapping
    public ApiResult<List<Category>> list() {
        return ApiResult.ok(categoryMapper.selectList(Wrappers.emptyWrapper()));
    }
}

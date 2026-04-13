package com.seckill.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.dto.ApiResult;
import com.seckill.dto.SessionUser;
import com.seckill.entity.Product;
import com.seckill.interceptor.LoginInterceptor;
import com.seckill.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品公开读接口：分页与详情均无需登录（拦截器排除）。详情接口若带 Session 且已登录，则把用户 ID 传给服务层记浏览日志。
 * 查询参数 {@code q} 与 {@code keyword} 二选一，兼容不同前端命名。
 * 前端调用关系：由 {@code frontend/src/api/product.js} 调用；
 * {@code fetchProducts()} 被 Home.vue/Search.vue 使用，{@code fetchProduct()} 被 ProductDetail.vue 使用。
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 上架商品分页；支持分类与关键词（q 与 keyword 二选一）。 */
    @GetMapping
    public ApiResult<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String keyword) {
        String searchText = (q != null && !q.isBlank()) ? q : keyword;
        IPage<Product> p = productService.page(page, size, categoryId, searchText);
        Map<String, Object> m = new HashMap<>();
        m.put("records", p.getRecords());
        m.put("total", p.getTotal());
        m.put("current", p.getCurrent());
        m.put("size", p.getSize());
        return ApiResult.ok(m);
    }

    /** 商品详情；已登录则记录浏览日志供推荐。 */
    @GetMapping("/{id}")
    public ApiResult<Product> detail(@PathVariable Long id, HttpSession session) {
        Long uid = null;
        if (session != null) {
            SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
            if (su != null) {
                uid = su.getId();
            }
        }
        Product p = productService.detail(id, uid);
        if (p == null) {
            return ApiResult.badRequest("商品不存在");
        }
        return ApiResult.ok(p);
    }
}

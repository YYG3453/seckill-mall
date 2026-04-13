package com.seckill.controller;

import com.seckill.dto.ApiResult;
import com.seckill.dto.CartLine;
import com.seckill.dto.SessionUser;
import com.seckill.entity.Product;
import com.seckill.interceptor.LoginInterceptor;
import com.seckill.mapper.ProductMapper;
import com.seckill.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车 REST：所有接口需登录（见 {@link com.seckill.config.WebMvcConfig} 拦截配置）。
 * 列表接口会联表查询 {@link Product} 填充展示字段；已下架商品若仍在 Hash 中仍会返回（前端可自行过滤）。
 * 前端调用关系：由 {@code frontend/src/api/cart.js} 调用；
 * fetchCart 被 Cart.vue/store/cart.js 使用，addCart 被 ProductDetail.vue 使用，其余更新/删除/勾选被 Cart.vue 使用。
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ProductMapper productMapper;

    /** 购物车行列表：合并 Redis 行与商品详情；已删除商品跳过。 */
    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        Map<Long, CartLine> map = cartService.list(su.getId());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<Long, CartLine> e : map.entrySet()) {
            Product p = productMapper.selectById(e.getKey());
            if (p == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("productId", e.getKey());
            row.put("quantity", e.getValue().getQuantity());
            row.put("selected", e.getValue().isSelected());
            row.put("product", p);
            out.add(row);
        }
        return ApiResult.ok(out);
    }

    /** 增加某商品数量，默认加 1。 */
    @PostMapping("/add")
    public ApiResult<Void> add(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity,
                               HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        cartService.add(su.getId(), productId, quantity);
        return ApiResult.ok(null);
    }

    /** 设置行数量；≤0 时服务层删除该行。 */
    @PutMapping("/qty")
    public ApiResult<Void> qty(@RequestParam Long productId, @RequestParam int quantity, HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        cartService.updateQty(su.getId(), productId, quantity);
        return ApiResult.ok(null);
    }

    /** 移除购物车中指定商品。 */
    @DeleteMapping("/{productId}")
    public ApiResult<Void> remove(@PathVariable Long productId, HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        cartService.remove(su.getId(), productId);
        return ApiResult.ok(null);
    }

    /** 勾选/取消勾选，影响结算时是否计入。 */
    @PutMapping("/select")
    public ApiResult<Void> select(@RequestParam Long productId, @RequestParam boolean selected, HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        cartService.toggle(su.getId(), productId, selected);
        return ApiResult.ok(null);
    }
}

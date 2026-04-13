package com.seckill.controller;

import com.seckill.dto.ApiResult;
import com.seckill.dto.SessionUser;
import com.seckill.entity.OrderEntity;
import com.seckill.entity.OrderItem;
import com.seckill.interceptor.LoginInterceptor;
import com.seckill.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户订单接口：从购物车生成订单、模拟支付、列表与详情（含明细）。
 * Session 中的 {@link com.seckill.dto.SessionUser} 由登录接口写入，拦截器保证已登录。
 * 前端调用关系：由 {@code frontend/src/api/order.js} 调用；
 * createOrderFromCart 被 Cart.vue 使用，payOrder/fetchOrders/fetchOrderDetail 被 OrderList.vue 使用。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 将购物车已勾选行生成一笔「待支付」订单并扣减商品库存。 */
    @PostMapping("/from-cart")
    public ApiResult<Map<String, String>> fromCart(HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        String orderNo = orderService.createFromCart(su.getId());
        return ApiResult.okMsg("下单成功", Map.of("orderNo", orderNo));
    }

    /** 演示环境模拟支付：仅「待支付」可付。 */
    @PostMapping("/{orderNo}/pay")
    public ApiResult<Void> pay(@PathVariable String orderNo, HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        orderService.simulatePay(su.getId(), orderNo);
        return ApiResult.okMsg("支付成功", null);
    }

    /** 当前用户订单列表。 */
    @GetMapping
    public ApiResult<List<OrderEntity>> list(HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        return ApiResult.ok(orderService.listByUser(su.getId()));
    }

    /** 订单详情 + 明细行；订单须属于当前用户。 */
    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id, HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        OrderEntity o = orderService.detailByUser(su.getId(), id);
        List<OrderItem> items = orderService.items(o.getId());
        Map<String, Object> m = new HashMap<>();
        m.put("order", o);
        m.put("items", items);
        return ApiResult.ok(m);
    }
}

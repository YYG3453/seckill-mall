package com.seckill.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.dto.ApiResult;
import com.seckill.entity.*;
import com.seckill.mapper.*;
import com.seckill.service.AdminStatisticsService;
import com.seckill.service.FileStorageService;
import com.seckill.service.MonitorService;
import com.seckill.service.OrderService;
import com.seckill.service.ProductService;
import com.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端聚合 API：商品/分类/用户/秒杀场次 CRUD、订单发货、监控指标、统计看板、图片上传。
 * 全部由 {@link com.seckill.interceptor.LoginInterceptor} 强制要求 {@code role=admin}（路径含 {@code /api/admin}）。
 * 前端调用关系：由 {@code frontend/src/api/admin.js} 统一调用；
 * AdminProducts.vue/AdminUsers.vue/AdminOrders.vue/SeckillEventManage.vue/AdminDashboard.vue 分别消费对应接口。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final SeckillEventMapper seckillEventMapper;
    private final ProductService productService;
    private final SeckillService seckillService;
    private final MonitorService monitorService;
    private final OrderService orderService;
    private final FileStorageService fileStorageService;
    private final AdminStatisticsService adminStatisticsService;

    /** 商品图上传，返回 /uploads/products/... 形式 URL。 */
    @PostMapping("/upload/image")
    public ApiResult<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeImage(file, "products");
        return ApiResult.ok(Map.of("url", url));
    }

    /** 后台商品列表（含下架），按 ID 倒序。 */
    @GetMapping("/products")
    public ApiResult<List<Product>> products() {
        return ApiResult.ok(productMapper.selectList(
                Wrappers.<Product>lambdaQuery().orderByDesc(Product::getId)));
    }

    /** 新建商品；补默认上架状态与创建时间。 */
    @PostMapping("/products")
    public ApiResult<Void> createProduct(@RequestBody Product body) {
        body.setId(null);
        if (body.getCreateTime() == null) {
            body.setCreateTime(LocalDateTime.now());
        }
        if (body.getStatus() == null) {
            body.setStatus(1);
        }
        productMapper.insert(body);
        return ApiResult.ok(null);
    }

    /** 更新商品；刷新详情缓存避免前台读到旧数据。 */
    @PutMapping("/products/{id}")
    public ApiResult<Void> updateProduct(@PathVariable Long id, @RequestBody Product body) {
        body.setId(id);
        productMapper.updateById(body);
        productService.evictDetailCache(id);
        return ApiResult.ok(null);
    }

    /** 上架/下架等状态变更；同步失效 Redis 商品详情缓存。 */
    @PutMapping("/products/{id}/status")
    public ApiResult<Void> productStatus(@PathVariable Long id, @RequestParam int status) {
        Product p = productMapper.selectById(id);
        if (p != null) {
            p.setStatus(status);
            productMapper.updateById(p);
            productService.evictDetailCache(id);
        }
        return ApiResult.ok(null);
    }

    /** 全部分类，供后台表单下拉。 */
    @GetMapping("/categories")
    public ApiResult<List<Category>> adminCategories() {
        return ApiResult.ok(categoryMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.emptyWrapper()));
    }

    /** 新建分类。 */
    @PostMapping("/categories")
    public ApiResult<Void> createCategory(@RequestBody Category c) {
        c.setId(null);
        categoryMapper.insert(c);
        return ApiResult.ok(null);
    }

    /** 订单分页列表，管理端查看。 */
    @GetMapping("/orders")
    public ApiResult<List<OrderEntity>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResult.ok(orderMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                Wrappers.<OrderEntity>lambdaQuery().orderByDesc(OrderEntity::getCreateTime)
        ).getRecords());
    }

    /** 指定订单的明细行。 */
    @GetMapping("/orders/{id}/items")
    public ApiResult<List<OrderItem>> orderItems(@PathVariable Long id) {
        return ApiResult.ok(orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, id)));
    }

    /** 对已支付订单发货；失败返回业务错误文案。 */
    @PostMapping("/orders/{orderNo}/ship")
    public ApiResult<Void> ship(@PathVariable String orderNo) {
        try {
            orderService.ship(orderNo);
        } catch (Exception ex) {
            return ApiResult.badRequest(ex.getMessage());
        }
        return ApiResult.ok(null);
    }

    /** 用户列表，按 ID 倒序。 */
    @GetMapping("/users")
    public ApiResult<List<User>> users() {
        return ApiResult.ok(userMapper.selectList(
                Wrappers.<User>lambdaQuery().orderByDesc(User::getId)));
    }

    /** 启用/禁用用户账号。 */
    @PutMapping("/users/{id}/status")
    public ApiResult<Void> userStatus(@PathVariable Long id, @RequestParam int status) {
        User u = userMapper.selectById(id);
        if (u != null) {
            u.setStatus(status);
            userMapper.updateById(u);
        }
        return ApiResult.ok(null);
    }

    /** 秒杀场次列表。 */
    @GetMapping("/seckill/events")
    public ApiResult<List<SeckillEvent>> seckillList() {
        return ApiResult.ok(seckillEventMapper.selectList(
                Wrappers.<SeckillEvent>lambdaQuery().orderByDesc(SeckillEvent::getId)));
    }

    /** 新建场次；若启用且未结束则同步 Redis。 */
    @PostMapping("/seckill/events")
    public ApiResult<Void> seckillCreate(@RequestBody SeckillEvent e) {
        e.setId(null);
        seckillEventMapper.insert(e);
        if (e.getStatus() != null && e.getStatus() == 1 && e.getEndTime().isAfter(LocalDateTime.now())) {
            seckillService.syncToRedis(e);
        }
        return ApiResult.ok(null);
    }

    /** 更新场次；按最新 DB 状态决定 sync Redis 或清理 Redis。 */
    @PutMapping("/seckill/events/{id}")
    public ApiResult<Void> seckillUpdate(@PathVariable Long id, @RequestBody SeckillEvent e) {
        e.setId(id);
        seckillEventMapper.updateById(e);
        SeckillEvent db = seckillEventMapper.selectById(id);
        if (db != null && db.getStatus() != null && db.getStatus() == 1
                && db.getEndTime().isAfter(LocalDateTime.now())) {
            seckillService.syncToRedis(db);
        } else {
            seckillService.removeFromRedis(id);
        }
        return ApiResult.ok(null);
    }

    /** 软删场次（status=0）并移除 Redis 中该场次数据。 */
    @DeleteMapping("/seckill/events/{id}")
    public ApiResult<Void> seckillDelete(@PathVariable Long id) {
        SeckillEvent e = seckillEventMapper.selectById(id);
        if (e != null) {
            e.setStatus(0);
            seckillEventMapper.updateById(e);
        }
        seckillService.removeFromRedis(id);
        return ApiResult.ok(null);
    }

    /** 最近 60 分钟监控曲线数据（QPS 等）。 */
    @GetMapping("/monitor/stats")
    public ApiResult<Map<String, Object>> monitorStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("series", monitorService.lastHourStats());
        return ApiResult.ok(m);
    }

    /** 经营看板：订单、销售额、分类占比、日 K 等聚合指标。 */
    @GetMapping("/statistics/dashboard")
    public ApiResult<Map<String, Object>> statisticsDashboard() {
        return ApiResult.ok(adminStatisticsService.dashboard());
    }
}

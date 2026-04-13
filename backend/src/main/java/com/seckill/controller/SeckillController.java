package com.seckill.controller;

import com.seckill.annotation.RateLimit;
import com.seckill.dto.ApiResult;
import com.seckill.dto.SessionUser;
import com.seckill.entity.SeckillReminder;
import com.seckill.interceptor.LoginInterceptor;
import com.seckill.mapper.SeckillEventMapper;
import com.seckill.mapper.SeckillReminderMapper;
import com.seckill.entity.SeckillEvent;
import com.seckill.service.MonitorService;
import com.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 秒杀对外 HTTP 层：公开查询（无需登录）、库存查询、动态下单路径、真正扣减（Lua+DB）、开抢提醒。
 * <p>
 * 安全与限流要点：
 * <ul>
 *   <li>真正下单接口为 {@code POST /api/seckill/{path}/do}，path 由已登录用户调用 {@code GET /path/{itemId}} 获取，短期有效，避免脚本直接 POST 固定 URL。</li>
 *   <li>{@link com.seckill.annotation.RateLimit} 限制每用户/每 IP 在固定窗口内的请求次数，配合 Redis 计数。</li>
 *   <li>需要登录的接口依赖 Session（与 {@link com.seckill.interceptor.LoginInterceptor} 排除列表配合：公开接口已排除）。</li>
 * </ul>
 * 前端调用关系：由 {@code frontend/src/api/seckill.js} 调用；
 * ProductDetail.vue 使用 by-product/stock/path/do/remind，Home.vue 使用 event/public，SeckillCountdown.vue 通过 common/now 对时。
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;
    private final MonitorService monitorService;
    private final SeckillReminderMapper seckillReminderMapper;
    private final SeckillEventMapper seckillEventMapper;

    /** 按秒杀场次 ID 返回前台展示用信息（价格、时间、当前 Redis 库存等），无登录要求。 */
    @GetMapping("/event/public/{itemId}")
    public ApiResult<Map<String, Object>> publicEvent(@PathVariable Long itemId) {
        Map<String, Object> ev = seckillService.publicEvent(itemId);
        if (ev == null) {
            return ApiResult.ok(null);
        }
        return ApiResult.ok(ev);
    }

    /** 商品详情页用：根据商品 ID 查「当前有效且未结束」的一场秒杀（若有），便于一个商品只挂一场时的展示。 */
    @GetMapping("/by-product/{productId}")
    public ApiResult<Map<String, Object>> byProduct(@PathVariable Long productId) {
        Map<String, Object> ev = seckillService.publicEventByProductId(productId);
        return ApiResult.ok(ev);
    }

    /** 轮询当前 Redis 中的剩余库存；若 key 不存在则由服务层回源 DB。 */
    @GetMapping("/stock/{itemId}")
    public ApiResult<Map<String, Integer>> stock(@PathVariable Long itemId) {
        int s = seckillService.currentRedisStock(itemId);
        return ApiResult.ok(Map.of("stock", s));
    }

    /**
     * 抢购前一步：为当前用户+场次生成随机 path 写入 Redis（短 TTL），前端需用返回的 path 调用 {@code /{path}/do}。
     */
    @GetMapping("/path/{itemId}")
    public ApiResult<Map<String, String>> path(@PathVariable Long itemId, HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        String p = seckillService.generatePath(su.getId(), itemId);
        return ApiResult.ok(Map.of("path", p));
    }

    /**
     * 动态路径校验：path 仅通过 /path 接口下发并短期有效，避免秒杀 URL 被静态传播后直接脚本 POST。
     */
    @PostMapping("/{path}/do")
    @RateLimit(userLimit = 5, ipLimit = 10, periodSeconds = 60)
    public ApiResult<Map<String, String>> doSeckill(@PathVariable String path,
                                                    @RequestParam Long itemId,
                                                    HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        String orderNo = seckillService.doSeckill(su.getId(), path, itemId, monitorService);
        return ApiResult.okMsg("秒杀成功，请前往支付", Map.of("orderNo", orderNo));
    }

    /**
     * 预约开抢提醒：校验场次启用后插入提醒表（status=0），由 {@link com.seckill.task.SeckillReminderTask} 扫描并发通知。
     * 此处直接使用 Mapper 简化演示；若业务变复杂可抽到独立 Service。
     */
    @PostMapping("/remind/{itemId}")
    public ApiResult<Void> remind(@PathVariable Long itemId, HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        SeckillEvent ev = seckillEventMapper.selectById(itemId);
        if (ev == null || ev.getStatus() == null || ev.getStatus() != 1) {
            return ApiResult.badRequest("场次不存在");
        }
        SeckillReminder r = new SeckillReminder();
        r.setUserId(su.getId());
        r.setItemId(itemId);
        r.setSeckillTime(ev.getStartTime());
        r.setStatus(0);
        seckillReminderMapper.insert(r);
        return ApiResult.ok(null);
    }
}

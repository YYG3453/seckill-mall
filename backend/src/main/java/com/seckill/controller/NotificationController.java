package com.seckill.controller;

import com.seckill.dto.ApiResult;
import com.seckill.dto.SessionUser;
import com.seckill.entity.UserNotification;
import com.seckill.interceptor.LoginInterceptor;
import com.seckill.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 用户站内通知：未读列表、单条已读；需登录（走拦截器）。数据由秒杀提醒等任务写入。
 * 前端调用关系：由 {@code frontend/src/api/notification.js} 调用，主要在 MainLayout.vue 导航栏消息模块轮询与已读。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 当前用户未读通知，最多 50 条，按时间倒序。 */
    @GetMapping("/unread")
    public ApiResult<List<UserNotification>> unread(HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        return ApiResult.ok(notificationService.unread(su.getId()));
    }

    /** 将指定通知标为已读；非本人通知则服务层静默忽略。 */
    @PostMapping("/{id}/read")
    public ApiResult<Void> read(@PathVariable Long id, HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        notificationService.markRead(su.getId(), id);
        return ApiResult.ok(null);
    }
}

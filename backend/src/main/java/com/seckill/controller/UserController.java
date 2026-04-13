package com.seckill.controller;

import com.seckill.dto.ApiResult;
import com.seckill.dto.SessionUser;
import com.seckill.dto.UserLoginRequest;
import com.seckill.dto.UserProfileUpdateRequest;
import com.seckill.dto.UserRegisterRequest;
import com.seckill.entity.OrderEntity;
import com.seckill.interceptor.LoginInterceptor;
import com.seckill.service.FileStorageService;
import com.seckill.service.OrderService;
import com.seckill.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户账户：注册/登录（写 Session）、登出、个人资料、头像上传（{@link com.seckill.service.FileStorageService}）、订单列表复用 {@link OrderService}。
 * 注册与登录路径在 {@link com.seckill.config.WebMvcConfig} 中从登录拦截排除。
 * 前端调用关系：由 {@code frontend/src/api/user.js} 调用；
 * Register.vue/Login.vue/Profile.vue、MainLayout.vue/AdminLayout.vue 及 store/user.js 均会使用本控制器接口。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OrderService orderService;
    private final FileStorageService fileStorageService;

    /** 用户注册；用户名唯一。 */
    @PostMapping("/register")
    public ApiResult<Void> register(@Valid @RequestBody UserRegisterRequest req) {
        userService.register(req);
        return ApiResult.ok(null);
    }

    /** 登录成功写入 Session，返回精简用户信息（含 role）。 */
    @PostMapping("/login")
    public ApiResult<SessionUser> login(@Valid @RequestBody UserLoginRequest req, HttpSession session) {
        return ApiResult.ok(userService.login(req, session));
    }

    /** 销毁 Session。 */
    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpSession session) {
        userService.logout(session);
        return ApiResult.ok(null);
    }

    /** 当前登录用户资料摘要，供前端恢复状态。 */
    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me(HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        if (su == null) {
            return ApiResult.ok(null);
        }
        var u = userService.profile(su.getId());
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("phone", u.getPhone());
        m.put("avatar", u.getAvatar());
        m.put("role", u.getRole());
        return ApiResult.ok(m);
    }

    /** 上传头像图片，存盘并更新用户 avatar 字段，返回可访问 URL。 */
    @PostMapping("/avatar")
    public ApiResult<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        String url = fileStorageService.storeImage(file, "avatars");
        userService.updateAvatar(su.getId(), url);
        return ApiResult.ok(Map.of("url", url));
    }

    /** 更新手机、头像 URL 等资料字段。 */
    @PutMapping("/profile")
    public ApiResult<Void> profile(@Valid @RequestBody UserProfileUpdateRequest req, HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        userService.updateProfile(su.getId(), req);
        return ApiResult.ok(null);
    }

    /** 当前用户订单列表（与 /api/orders GET 数据一致，路径不同便于个人中心调用）。 */
    @GetMapping("/orders")
    public ApiResult<List<OrderEntity>> myOrders(HttpSession session) {
        SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        return ApiResult.ok(orderService.listByUser(su.getId()));
    }
}

package com.seckill.controller;

import com.seckill.dto.AiChatRequest;
import com.seckill.dto.ApiResult;
import com.seckill.dto.SessionUser;
import com.seckill.entity.Product;
import com.seckill.interceptor.LoginInterceptor;
import com.seckill.service.AiChatService;
import com.seckill.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * AI 与推荐：推荐列表可匿名（session 有用户则个性化）；对话接口可走配置的大模型或本地规则，详见 {@link com.seckill.service.AiChatService}。
 * 部分路径在 {@link com.seckill.config.WebMvcConfig} 中从登录拦截排除，便于未登录浏览页面试用。
 * 前端调用关系：由 {@code frontend/src/api/ai.js} 发起；其中 {@code fetchRecommend()} 被 Home.vue 使用，
 * {@code chatAi()} 被 AIChatBot.vue 使用。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final RecommendService recommendService;
    private final AiChatService aiChatService;

    /**
     * 个性化推荐：未登录 {@code uid=null} 时走冷启动策略；已登录则结合浏览/购买日志与 Redis 相似度。
     */
    @GetMapping("/recommend")
    public ApiResult<List<Product>> recommend(HttpSession session) {
        Long uid = null;
        if (session != null) {
            SessionUser su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
            if (su != null) {
                uid = su.getId();
            }
        }
        return ApiResult.ok(recommendService.recommend(uid));
    }

    /**
     * 智能对话：body 可空；userId 从 Session 取，注入 {@link AiChatService} 用于大模型 system 提示与本地规则。
     */
    @PostMapping("/chat")
    public ApiResult<Map<String, String>> chat(@RequestBody(required = false) AiChatRequest body, HttpSession session) {
        SessionUser su = null;
        if (session != null) {
            su = (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        }
        // 传入完整 SessionUser，便于 AI 按 role 注入全站汇总；每次请求内会重新查库生成快照
        String ans = aiChatService.answer(body, su);
        return ApiResult.ok(Map.of("answer", ans));
    }
}

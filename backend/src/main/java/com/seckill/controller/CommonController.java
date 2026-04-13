package com.seckill.controller;

import com.seckill.dto.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 公共小接口：如服务器当前时间，供前端对时或演示；路径在拦截器中排除，游客可访问。
 * 前端调用关系：当前由 {@code frontend/src/api/seckill.js} 的 {@code fetchServerNow()} 调用，
 * 使用方为 SeckillCountdown.vue（秒杀倒计时与服务端时间对齐）。
 */
@RestController
@RequestMapping("/api/common")
public class CommonController {

    /** 返回当前 UTC 毫秒时间戳（epochMs）。 */
    @GetMapping("/now")
    public ApiResult<Map<String, Long>> now() {
        return ApiResult.ok(Map.of("epochMs", Instant.now().toEpochMilli()));
    }
}

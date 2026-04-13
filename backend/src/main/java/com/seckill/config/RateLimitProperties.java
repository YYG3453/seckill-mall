package com.seckill.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code seckill.rate-limit.*}：默认全局限流参数（当前工程秒杀接口主要用 {@link com.seckill.annotation.RateLimit} 注解覆盖）。
 */
@Data
@ConfigurationProperties(prefix = "seckill.rate-limit")
public class RateLimitProperties {
    private int userLimit = 5;
    private int userPeriodSeconds = 60;
    private int ipLimit = 10;
    private int ipPeriodSeconds = 60;
}

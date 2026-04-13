package com.seckill.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 {@link RateLimitProperties}；实际限流逻辑在 {@link com.seckill.aspect.RateLimitAspect} 与注解参数上。
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {
}

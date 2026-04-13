package com.seckill.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 {@link MonitorProperties}，供 {@link com.seckill.service.MonitorService} 读取告警阈值。
 */
@Configuration
@EnableConfigurationProperties(MonitorProperties.class)
public class MonitorConfig {
}

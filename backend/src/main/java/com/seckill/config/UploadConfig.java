package com.seckill.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 {@link UploadProperties}，供 {@link com.seckill.service.FileStorageService} 读取上传根路径。
 */
@Configuration
@EnableConfigurationProperties(UploadProperties.class)
public class UploadConfig {
}

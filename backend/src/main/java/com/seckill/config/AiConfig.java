package com.seckill.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * AI 相关 Bean：启用 {@link AiChatProperties}，并提供专用于大模型 HTTP 的 {@link RestTemplate}（较长读超时）。
 */
@Configuration
@EnableConfigurationProperties(AiChatProperties.class)
public class AiConfig {

    /** 连接 15s、读取 60s，避免模型慢响应过早失败。 */
    @Bean
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(15_000);
        f.setReadTimeout(60_000);
        return new RestTemplate(f);
    }
}

package com.seckill.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 {@code application.yml} 中 {@code app.ai.*}：是否启用、API Key、Base URL（需含 /v1）、模型名。
 */
@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiChatProperties {

    private boolean openaiEnabled = false;

    private String openaiApiKey = "";

    private String openaiBaseUrl = "https://api.openai.com/v1";

    private String openaiModel = "gpt-4o-mini";
}

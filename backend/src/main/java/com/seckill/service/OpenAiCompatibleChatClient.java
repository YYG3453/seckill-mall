package com.seckill.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.config.AiChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用 OpenAI 兼容的 {@code POST /chat/completions}（如通义、OpenAI）。
 * 未启用、缺 Key、HTTP 失败或返回空内容时一律返回 {@code null}，由 {@link com.seckill.service.AiChatService} 走本地规则。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleChatClient {

    private static final String SYSTEM = """
            你是「秒杀商城」的在线客服，昵称小秒。语气自然、口语化，像真人朋友聊天；回答简洁分点，避免机械列举关键词。
            业务范围：秒杀规则、购物车、订单与模拟支付、发货、登录注册、导航入口。不涉及的功能请诚实说演示版暂无。
            若用户消息附带「系统补充」中的【实时数据快照】，其中数字为服务端刚查库的结果，请优先据此回答订单/库存/用户量等问题，勿与其他账号混淆。
            """;

    private final AiChatProperties props;
    private final RestTemplate aiRestTemplate;
    private final ObjectMapper objectMapper;

    /** 无额外 system 提示时的便捷重载。 */
    public String complete(String userText, List<ChatTurn> history) {
        return complete(userText, history, null);
    }

    /**
     * 组装 system + 历史多轮 + 当前 user 消息，POST 到远端；解析 {@code choices[0].message.content}。
     *
     * @param extraSystemHint 可选，追加在人设后（如订单笔数），减少幻觉
     * @return 模型正文；不可用场景返回 null
     */
    public String complete(String userText, List<ChatTurn> history, String extraSystemHint) {
        if (!props.isOpenaiEnabled()) {
            return null;
        }
        String key = props.getOpenaiApiKey();
        if (key == null || key.isBlank()) {
            return null;
        }
        String base = props.getOpenaiBaseUrl() == null ? "" : props.getOpenaiBaseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/chat/completions";

        String systemContent = SYSTEM;
        if (extraSystemHint != null && !extraSystemHint.isBlank()) {
            systemContent = SYSTEM + "\n\n【系统补充】" + extraSystemHint.trim();
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemContent));
        if (history != null) {
            for (ChatTurn t : history) {
                if (t == null || t.text() == null || t.text().isBlank()) {
                    continue;
                }
                String role = "user".equalsIgnoreCase(t.role()) ? "user" : "assistant";
                messages.add(Map.of("role", role, "content", truncate(t.text(), 800)));
            }
        }
        messages.add(Map.of("role", "user", "content", truncate(userText, 2000)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getOpenaiModel());
        body.put("messages", messages);
        body.put("temperature", 0.7);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key.trim());
            String raw = aiRestTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            if (raw == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choice = root.path("choices").path(0).path("message").path("content");
            String out = choice.asText("");
            return out.isBlank() ? null : out.trim();
        } catch (Exception e) {
            log.warn("OpenAI compatible chat failed: {}", e.getMessage());
            return null;
        }
    }

    /** 限制单条消息长度，避免超长上下文与费用。 */
    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    /** 一轮对话：role 为 user/assistant 等，text 为内容。 */
    public record ChatTurn(String role, String text) {
    }
}

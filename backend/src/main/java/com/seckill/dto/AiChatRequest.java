package com.seckill.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 对话请求体。
 * 功能：承载用户本轮提问 text 与多轮历史 history，供后端拼接上下文并调用大模型。
 * 创建原因：将聊天接口入参结构化，避免直接使用 Map 导致字段不稳定与校验困难。
 */
@Data
public class AiChatRequest {

    private String text;

    /** 当前用户输入之前的对话，role 为 user 或 assistant */
    private List<Message> history;

    @Data
    public static class Message {
        private String role;
        private String text;
    }
}

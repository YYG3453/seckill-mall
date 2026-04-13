package com.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 会话用户快照 DTO。
 * 功能：在 HttpSession 中保存最小登录态（id/username/role），供拦截器和业务快速读取。
 * 创建原因：避免把完整 User 实体（含敏感字段）直接放入会话，提高安全性并减少 Session 体积。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String role;
}

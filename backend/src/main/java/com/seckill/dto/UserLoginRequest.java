package com.seckill.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录接口入参 DTO。
 * 功能：封装用户名与密码，并通过校验注解约束非空。
 * 创建原因：把登录请求参数显式建模，便于参数校验、文档化和后续扩展（验证码等）。
 */
@Data
public class UserLoginRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}

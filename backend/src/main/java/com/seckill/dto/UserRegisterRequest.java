package com.seckill.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 注册接口入参 DTO。
 * 功能：封装用户名、密码、手机号并声明长度约束。
 * 创建原因：让注册参数在进入业务层前完成基础校验，减少 Service 中的样板判空与边界判断。
 */
@Data
public class UserRegisterRequest {
    @NotBlank
    @Size(max = 50)
    private String username;
    @NotBlank
    @Size(min = 6, max = 64)
    private String password;
    @Size(max = 20)
    private String phone;
}

package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（对应 user 表）。
 * 功能：保存账号、密码哈希、角色、状态、头像等资料字段。
 * 创建原因：统一用户持久化模型，供登录鉴权、个人中心与后台用户管理复用。
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String phone;
    private String avatar;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
}

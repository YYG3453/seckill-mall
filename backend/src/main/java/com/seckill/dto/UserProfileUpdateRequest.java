package com.seckill.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 用户资料更新入参 DTO。
 * 功能：承载可修改字段（手机号、头像 URL）及长度限制。
 * 创建原因：将“可改字段白名单”集中在一个对象中，防止前端提交多余字段误更新。
 */
@Data
public class UserProfileUpdateRequest {
    @Size(max = 20)
    private String phone;

    /** 头像地址，可为上传接口返回的 /uploads/... 或外链 */
    @Size(max = 500)
    private String avatar;
}

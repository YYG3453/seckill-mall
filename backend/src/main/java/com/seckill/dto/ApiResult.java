package com.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回包装体。
 * 功能：把控制器返回值标准化为 code/msg/data 三段，便于前后端约定与错误处理统一。
 * 创建原因：避免各接口返回结构不一致，降低前端解析与全局拦截复杂度。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(200, "success", data);
    }

    public static <T> ApiResult<T> okMsg(String msg, T data) {
        return new ApiResult<>(200, msg, data);
    }

    public static <T> ApiResult<T> fail(int code, String msg) {
        return new ApiResult<>(code, msg, null);
    }

    public static <T> ApiResult<T> badRequest(String msg) {
        return new ApiResult<>(400, msg, null);
    }

    public static <T> ApiResult<T> error(String msg) {
        return new ApiResult<>(500, msg, null);
    }
}

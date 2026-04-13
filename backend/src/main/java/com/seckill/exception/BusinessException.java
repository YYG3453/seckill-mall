package com.seckill.exception;

/**
 * 通用业务异常。
 * 功能：承载可预期的业务错误（参数不合法、状态不允许等），由全局异常处理统一返回友好消息。
 * 创建原因：区分系统异常与业务失败，避免业务分支使用 RuntimeException 导致语义不清。
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

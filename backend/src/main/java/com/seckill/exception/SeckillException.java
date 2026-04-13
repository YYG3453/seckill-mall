package com.seckill.exception;

/**
 * 秒杀域专用业务异常。
 * 功能：表达秒杀链路中的特定失败（库存不足、路径失效、不在活动时间等）。
 * 创建原因：让秒杀错误在日志与返回层面可被快速识别，并与通用业务异常做语义区分。
 */
public class SeckillException extends RuntimeException {
    public SeckillException(String message) {
        super(message);
    }
}

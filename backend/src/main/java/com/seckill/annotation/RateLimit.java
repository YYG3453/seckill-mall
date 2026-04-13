package com.seckill.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解。
 * 功能：在方法上声明用户维/IP 维限流阈值与窗口时间，交由 RateLimitAspect 执行。
 * 创建原因：将限流策略声明式下沉到接口层，避免在业务代码中硬编码限流逻辑。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int userLimit() default 5;

    int ipLimit() default 10;

    int periodSeconds() default 60;
}

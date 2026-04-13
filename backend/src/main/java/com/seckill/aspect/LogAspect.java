package com.seckill.aspect;

import com.seckill.dto.SessionUser;
import com.seckill.interceptor.LoginInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 秒杀模块访问日志切面：在 {@link com.seckill.controller.SeckillController} 每个方法执行前后「环绕」，
 * 在<strong>执行前</strong>解析当前登录用户 ID 并打印一行 info 日志，再<strong>原样调用</strong>目标方法。
 * <p>
 * 与 {@link RateLimitAspect} 区别：本类只做观测（日志），不改变业务；限流切面可能抛异常阻断请求。
 */
@Slf4j // 生成 log 字段，下面 log.info 使用
@Aspect // 标识为切面，由 Spring AOP 代理 SeckillController 时织入
@Component // 注册为 Spring Bean
public class LogAspect {

    /**
     * 环绕通知：
     * <ul>
     *   <li>{@code execution(* com.seckill.controller.SeckillController.*(..))} 表示匹配该类型下任意方法、任意参数。</li>
     *   <li>{@link ProceedingJoinPoint#proceed()} 必须调用，否则目标 Controller 方法不会执行。</li>
     * </ul>
     *
     * @param pjp 连接点，可获取签名、参数、目标对象；proceed() 用于继续执行被拦截方法
     * @return 与被拦截方法相同的返回值，原样返回给调用方
     */
    @Around("execution(* com.seckill.controller.SeckillController.*(..))")
    public Object aroundSeckill(ProceedingJoinPoint pjp) throws Throwable {
        // 与 RateLimitAspect 相同：从线程本地取当前请求绑定对象
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 用于日志：未取到 Session 或用户未登录时保持 null
        Long userId = null;
        if (attrs != null) {
            // 当前 HTTP 请求
            HttpServletRequest req = attrs.getRequest();
            // 不强制创建 Session，避免无状态请求产生多余 Session
            HttpSession s = req.getSession(false);
            if (s != null) {
                // 登录成功后 LoginInterceptor 或 UserService 会写入 SESSION_USER
                SessionUser u = (SessionUser) s.getAttribute(LoginInterceptor.SESSION_USER_KEY);
                if (u != null) {
                    userId = u.getId();
                }
            }
        }
        // 被拦截方法的简单名，如 publicEvent、doSeckill，便于日志检索
        String name = pjp.getSignature().getName();
        // 结构化日志：占位符方式，避免字符串拼接
        log.info("seckill api={}, userId={}", name, userId);
        // 调用真正的 Controller 方法；其返回值可能是 ApiResult 等，直接返回给 DispatcherServlet
        return pjp.proceed();
    }
}

package com.seckill.aspect;

import com.seckill.annotation.RateLimit;
import com.seckill.dto.SessionUser;
import com.seckill.exception.BusinessException;
import com.seckill.interceptor.LoginInterceptor;
import com.seckill.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

/**
 * 固定窗口限流切面：在「带 {@link RateLimit} 注解」的 Controller 方法执行<strong>之前</strong>做计数。
 * <p>
 * 实现要点：
 * <ul>
 *   <li>用 Redis {@code INCR key} 做原子计数；key 在窗口内累加，超过阈值则抛 {@link BusinessException}，由全局异常处理返回友好文案。</li>
 *   <li>第一次 INCR 结果为 1 时给 key 设置 {@code EXPIRE periodSeconds}，形成「从第一次请求开始的固定时间窗」。</li>
 *   <li>同时限制「用户维度」和「IP 维度」：任一侧超限都会拦截，兼顾登录用户刷接口与匿名脚本按 IP 刷。</li>
 * </ul>
 */
@Slf4j // Lombok：生成名为 log 的 Slf4j 日志器，本类当前未打日志，保留便于后续排查限流
@Aspect // 声明这是一个 AspectJ 切面类，需配合 spring-boot-starter-aop 生效
@Component // 交给 Spring 扫描为 Bean，切面才能被织入
@RequiredArgsConstructor // Lombok：为所有 final 字段生成构造器，完成依赖注入
public class RateLimitAspect {

    // Redis 字符串操作：INCR、EXPIRE 均通过 opsForValue()
    private final StringRedisTemplate stringRedisTemplate;
    // 被限流时记一条监控指标，便于后台看「触发了多少次限流」
    private final MonitorService monitorService;

    /**
     * 前置通知：匹配「任意方法上标注了 @RateLimit，且把该注解绑定到参数名 rl」的切点。
     * <p>
     * {@code @Before("@annotation(rl)")} 中 rl 必须与参数名一致，AspectJ 会把当前方法上的注解实例注入进来，
     * 从而读到 {@link RateLimit#userLimit()}、{@link RateLimit#ipLimit()}、{@link RateLimit#periodSeconds()} 等属性。
     *
     * @param jp 连接点，可扩展为记录类名、参数等（当前未使用）
     * @param rl 目标方法上的 @RateLimit 注解实例
     */
    @Before("@annotation(rl)")
    public void before(JoinPoint jp, RateLimit rl) {
        // 从 Spring 的请求上下文取出当前线程绑定的 ServletRequestAttributes（仅在 Web 请求线程内非空）
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            // 非 Web 环境（如单元测试、异步线程未传递 Request）无法取到请求，直接放行避免 NPE
            return;
        }
        // 当前 HTTP 请求对象，用于取 Session、Header、真实 IP
        HttpServletRequest req = attrs.getRequest();
        // false：没有 Session 时不要新建，只返回 null（避免匿名访问时创建无用 Session）
        HttpSession session = req.getSession(false);
        // 与登录拦截器约定：Session 中该 key 存的是 SessionUser
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(LoginInterceptor.SESSION_USER_KEY);
        // 未登录则 uid 为 null，后面 Redis key 用 "anon" 占位，仍可对「未登录但打到需限流接口」的场景计数（若业务上该接口必登录则很少走到 anon）
        Long uid = user == null ? null : user.getId();
        // 解析客户端 IP，供 IP 维度限流
        String ip = clientIp(req);

        // 用户维度：key 形如 rate:seckill:user:123；限制次数取注解 userLimit，窗口秒数取 periodSeconds
        check("rate:seckill:user:" + (uid == null ? "anon" : uid), rl.userLimit(), rl.periodSeconds());
        // IP 维度：key 形如 rate:seckill:ip:192.168.1.1；限制次数取 ipLimit
        check("rate:seckill:ip:" + ip, rl.ipLimit(), rl.periodSeconds());
    }

    /**
     * 对单个 Redis key 做一次「窗口内计数 + 是否超限」判断。
     *
     * @param key           Redis 键，全项目限流需保证前缀/命名不冲突
     * @param limit         本窗口内允许的最大请求次数（超过则拒绝）
     * @param periodSeconds 窗口长度（秒），从<strong>该 key 第一次被 INCR</strong>开始计时
     */
    private void check(String key, int limit, int periodSeconds) {
        // INCR 返回递增后的值；key 不存在时 Redis 会先当作 0 再 INCR，故首次结果为 1
        Long c = stringRedisTemplate.opsForValue().increment(key);
        // 仅在「本窗口的第一次请求」时设置过期时间，后续请求只累加计数不改变 TTL（固定窗口语义）
        if (c != null && c == 1L) {
            stringRedisTemplate.expire(key, periodSeconds, TimeUnit.SECONDS);
        }
        // 当前计数严格大于 limit 则拒绝：例如 limit=5，则第 6 次及以后被拒绝（前 5 次允许）
        if (c != null && c > limit) {
            monitorService.recordRateLimited();
            // 业务异常由 GlobalExceptionHandler 统一转成 JSON，前端可提示「请求过于频繁」
            throw new BusinessException("请求过于频繁");
        }
    }

    /**
     * 获取客户端 IP：部署在 Nginx 等反向代理后时，真实 IP 常在 X-Forwarded-For 头（多段用逗号分隔，取第一段）。
     * 直连 Tomcat 时通常没有该头，退回 {@link HttpServletRequest#getRemoteAddr()}。
     */
    private static String clientIp(HttpServletRequest req) {
        // 标准代理追加的头，可能形如 "client, proxy1, proxy2"
        String x = req.getHeader("X-Forwarded-For");
        if (x != null && !x.isBlank()) {
            // 取链路上最左侧一段，一般是最原始客户端（具体以运维配置为准）
            return x.split(",")[0].trim();
        }
        // 无代理信息时使用 TCP 连接对端地址
        return req.getRemoteAddr();
    }
}

package com.seckill.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.dto.ApiResult;
import com.seckill.dto.SessionUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;

/**
 * 登录态拦截器：注册在 {@code /api/**} 上，具体放行路径见 {@link com.seckill.config.WebMvcConfig#addInterceptors}。
 * <p>
 * <b>处理顺序</b>：OPTIONS 预检直接放行 → 路径以 {@code /api/admin} 为前缀则要求已登录且 {@code role=admin}（401/403 JSON）→
 * 其余被拦截的请求要求 Session 中存在 {@link com.seckill.dto.SessionUser}，否则 401。
 * <p>
 * 因此「游客可访问」的接口必须通过 {@code excludePathPatterns} 排除（含 {@code /api/user/me} 未登录返回 data=null），否则会误拦。
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER_KEY = "SESSION_USER";

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri;
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            path = uri.substring(ctx.length());
        }
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(SESSION_USER_KEY);

        // 勿用 uri.contains("/api/admin")：会误伤含该子串的任意路径；仅匹配管理端前缀
        if (path.startsWith("/api/admin/") || "/api/admin".equals(path)) {
            if (user == null) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ApiResult.badRequest("请先登录"));
                return false;
            }
            if (!"admin".equals(user.getRole())) {
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, ApiResult.badRequest("无管理员权限"));
                return false;
            }
            return true;
        }

        if (user == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ApiResult.badRequest("请先登录"));
            return false;
        }
        return true;
    }

    private void writeJson(HttpServletResponse response, int httpStatus, ApiResult<?> body) throws Exception {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

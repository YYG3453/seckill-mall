package com.seckill.config;

import com.seckill.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import com.seckill.service.FileStorageService;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Web 层横切配置：CORS（允许前端 dev server 带 Cookie 访问 8081）、静态上传目录映射、登录拦截器白名单。
 * <p>
 * {@code /uploads/**} 映射到 {@link com.seckill.service.FileStorageService#getRootAbsolute()}，与 {@code storeImage} 返回的 URL 前缀一致。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final FileStorageService fileStorageService;

    /** 允许前端 8080 带 Cookie 访问 8081 的 /api，预检 OPTIONS 缓存 1 小时。 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8222", "http://127.0.0.1:9876")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowCredentials(true)
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = fileStorageService.getRootAbsolute();
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    /** 全 API 登录拦截 + 白名单排除（游客可浏览商品、分类、部分秒杀只读等）。 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 商品列表/详情、分类、部分秒杀只读接口、AI、注册登录等必须排除，否则游客无法浏览商城
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/register",
                        "/api/user/login",
                        // 未登录时返回 data=null，供路由守卫恢复会话，避免走拦截器 401
                        "/api/user/me",
                        "/api/products",
                        "/api/products/**",
                        "/api/common/**",
                        "/api/seckill/stock/**",
                        "/api/seckill/event/public/**",
                        "/api/seckill/by-product/**",
                        "/api/categories/**",
                        "/api/ai/recommend",
                        "/api/ai/chat"
                );
    }
}

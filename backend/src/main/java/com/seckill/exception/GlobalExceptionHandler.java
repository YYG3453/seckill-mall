package com.seckill.exception;

import com.seckill.dto.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;

/**
 * 全局异常到统一 JSON {@link com.seckill.dto.ApiResult} 的映射：业务/秒杀异常返回 200 + code 非成功；
 * 校验失败 400；数据库与 Redis 连接问题返回 500 并附带运维提示文案，便于本地排错。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBiz(BusinessException e) {
        log.warn("business: {}", e.getMessage());
        return ApiResult.badRequest(e.getMessage());
    }

    @ExceptionHandler(SeckillException.class)
    public ApiResult<Void> handleSeckill(SeckillException e) {
        log.warn("seckill: {}", e.getMessage());
        return ApiResult.badRequest(e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValid(Exception e) {
        String msg = "参数错误";
        if (e instanceof MethodArgumentNotValidException) {
            var ex = (MethodArgumentNotValidException) e;
            if (ex.getBindingResult().getFieldError() != null) {
                msg = ex.getBindingResult().getFieldError().getDefaultMessage();
            }
        } else if (e instanceof BindException) {
            var ex = (BindException) e;
            if (ex.getBindingResult().getFieldError() != null) {
                msg = ex.getBindingResult().getFieldError().getDefaultMessage();
            }
        }
        return ApiResult.badRequest(msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleCv(ConstraintViolationException e) {
        return ApiResult.badRequest(e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResult<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        log.warn("upload too large: {}", e.getMessage());
        return ApiResult.badRequest("上传文件过大，请小于 5MB");
    }

    @ExceptionHandler(MultipartException.class)
    public ApiResult<Void> handleMultipart(MultipartException e) {
        log.warn("multipart: {}", e.getMessage());
        return ApiResult.badRequest("文件上传格式错误，请使用 JPG/PNG/GIF/WEBP");
    }

    /**
     * 登录等接口依赖 MySQL；表字段与实体不一致（如缺少 avatar）也会在此类异常中体现。
     */
    @ExceptionHandler(DataAccessException.class)
    public ApiResult<Void> handleDataAccess(DataAccessException e, HttpServletResponse response) {
        log.error("database error", e);
        response.setStatus(500);
        return ApiResult.error(
                "数据库访问失败：请确认 MySQL 已启动，库名 seckill_mall、账号密码与 application.yml 一致；"
                        + "若 user 表缺少 avatar 列，请执行 db/alter_user_avatar.sql");
    }

    /**
     * Spring Session 使用 Redis 时，未启动 Redis 或端口/密码不对会导致登录写入 Session 失败。
     */
    @ExceptionHandler({RedisConnectionFailureException.class, RedisSystemException.class})
    public ApiResult<Void> handleRedis(Exception e, HttpServletResponse response) {
        log.error("redis error", e);
        response.setStatus(500);
        return ApiResult.error(
                "Redis 连接失败：请启动本机 Redis（默认 localhost:6379），并核对 application.yml 中 spring.redis 配置；"
                        + "本项目会话存在 Redis 中，Redis 不可用时会无法登录。");
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleOther(Exception e, HttpServletResponse response) {
        log.error("system error", e);
        response.setStatus(500);
        return ApiResult.error("系统繁忙，请稍后重试");
    }
}

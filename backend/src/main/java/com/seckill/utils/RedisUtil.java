package com.seckill.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 对 {@link StringRedisTemplate} 的薄封装：统一设置过期、自增等常用操作，业务代码可少写样板代码。
 * 若项目里更多地方直接用 Template，本类也可逐步被内联替代。
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    /** 写入字符串并指定秒级 TTL。 */
    public void setEx(String key, String value, long seconds) {
        stringRedisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    /** 读取字符串；不存在返回 null。 */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /** 删除键；不存在时返回 false。 */
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /** 原子自增 1，常用于计数器；键不存在时从 0 开始。 */
    public Long incr(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /** 为已存在的键设置过期秒数。 */
    public void expire(String key, long seconds) {
        stringRedisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }
}

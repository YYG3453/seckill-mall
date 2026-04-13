package com.seckill.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 基础设施：{@code RedisTemplate} 供需要复杂序列化的场景使用；{@code seckillLuaScript} 加载 classpath 下 {@code lua/seckill.lua}，
 * 返回类型 Long，与 {@link com.seckill.service.SeckillService} 中 {@code execute} 一致。
 */
@Configuration
public class RedisConfig {

    /**
     * 通用 RedisTemplate：值用 Jackson 序列化并带类型信息，适合存复杂对象；秒杀热点路径多用 {@link org.springframework.data.redis.core.StringRedisTemplate}。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        Jackson2JsonRedisSerializer<Object> jackson = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        jackson.setObjectMapper(om);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson);
        template.setHashValueSerializer(jackson);
        template.afterPropertiesSet();
        return template;
    }

    /** 秒杀原子脚本：KEYS[1]=库存，KEYS[2]=用户限购，ARGV[1]=限购 key TTL（秒）。 */
    @Bean
    public DefaultRedisScript<Long> seckillLuaScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new org.springframework.core.io.ClassPathResource("lua/seckill.lua"));
        script.setResultType(Long.class);
        return script;
    }
}

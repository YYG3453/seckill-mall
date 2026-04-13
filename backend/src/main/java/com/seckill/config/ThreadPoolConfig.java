package com.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 秒杀异步补偿在本方案中改为同步执行；保留线程池 Bean 供后续扩展或定时任务使用。
 */
@Configuration
public class ThreadPoolConfig {

    /** 通用异步线程池，Bean 名 {@code taskExecutor}，CallerRuns 拒绝策略防任务静默丢。 */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(500);
        ex.setThreadNamePrefix("seckill-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }
}

package com.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 启动入口。
 * 功能：启动应用上下文、扫描 Mapper、启用定时任务。
 * 创建原因：集中声明应用级基础能力，作为后端进程的统一启动点。
 */
@SpringBootApplication
@MapperScan("com.seckill.mapper")
@EnableScheduling
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}

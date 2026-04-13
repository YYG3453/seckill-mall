package com.seckill.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code seckill.monitor.*}：高 QPS 告警线、Redis/DB 库存允许偏差、连续 Lua 异常次数触发落库。
 */
@Data
@ConfigurationProperties(prefix = "seckill.monitor")
public class MonitorProperties {
    private int highQpsThreshold = 200;
    private int stockMismatchThreshold = 10;
    private int luaErrorConsecutive = 5;
}

package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为日志实体（对应 user_action_log 表）。
 * 功能：记录浏览/购买等行为用于推荐与运营分析。
 * 创建原因：将行为埋点持久化，支撑离线相似度计算与个性化推荐。
 */
@Data
@TableName("user_action_log")
public class UserActionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long itemId;
    private String actionType;
    private LocalDateTime createTime;
}

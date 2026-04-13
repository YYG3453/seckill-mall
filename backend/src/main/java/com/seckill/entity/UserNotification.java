package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体（对应 user_notification 表）。
 * 功能：保存发送给用户的消息内容、已读状态与创建时间。
 * 创建原因：统一承载系统通知（如秒杀提醒），支持前端未读列表与已读操作。
 */
@Data
@TableName("user_notification")
public class UserNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
}

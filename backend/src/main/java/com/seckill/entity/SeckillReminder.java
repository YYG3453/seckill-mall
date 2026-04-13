package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀提醒实体（对应 seckill_reminder 表）。
 * 功能：记录用户预约提醒的场次与提醒发送状态。
 * 创建原因：将“用户订阅提醒”持久化，供定时任务批量下发通知并防重复发送。
 */
@Data
@TableName("seckill_reminder")
public class SeckillReminder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long itemId;
    private LocalDateTime seckillTime;
    private Integer status;
}

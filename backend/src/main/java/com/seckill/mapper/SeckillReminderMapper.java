package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.SeckillReminder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀提醒 Mapper。
 * 功能：提供 SeckillReminder 表访问，支撑提醒预约登记与提醒任务状态更新。
 * 创建原因：将提醒订阅能力持久化，避免仅靠内存导致重启丢失。
 */
@Mapper
public interface SeckillReminderMapper extends BaseMapper<SeckillReminder> {
}

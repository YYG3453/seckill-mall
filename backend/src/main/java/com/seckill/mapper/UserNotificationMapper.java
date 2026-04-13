package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内通知 Mapper。
 * 功能：提供 UserNotification 的持久化访问，用于未读查询与已读更新。
 * 创建原因：通知功能需要独立表与访问层，支持定时任务写入、用户端读取。
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
}

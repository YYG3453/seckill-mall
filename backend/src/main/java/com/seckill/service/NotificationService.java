package com.seckill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.entity.UserNotification;
import com.seckill.mapper.UserNotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 站内通知查询与已读标记；表数据由秒杀提醒任务等写入。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserNotificationMapper userNotificationMapper;

    /** 未读通知列表，最多 50 条。 */
    public List<UserNotification> unread(Long userId) {
        return userNotificationMapper.selectList(
                Wrappers.<UserNotification>lambdaQuery()
                        .eq(UserNotification::getUserId, userId)
                        .eq(UserNotification::getIsRead, 0)
                        .orderByDesc(UserNotification::getCreateTime)
                        .last("limit 50"));
    }

    /** 仅当通知属于该用户时置已读。 */
    public void markRead(Long userId, Long id) {
        UserNotification n = userNotificationMapper.selectById(id);
        if (n != null && n.getUserId().equals(userId)) {
            n.setIsRead(1);
            userNotificationMapper.updateById(n);
        }
    }
}

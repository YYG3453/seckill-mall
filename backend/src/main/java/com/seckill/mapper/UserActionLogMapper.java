package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.UserActionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为日志 Mapper。
 * 功能：存取浏览/购买等行为记录，供推荐算法与运营分析使用。
 * 创建原因：把行为数据从业务主表中解耦，形成可扩展的推荐数据基础层。
 */
@Mapper
public interface UserActionLogMapper extends BaseMapper<UserActionLog> {
}

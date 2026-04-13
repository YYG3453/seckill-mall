package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.ExceptionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异常日志 Mapper。
 * 功能：将监控/异常信息写入 exception_log，并供后台查询展示。
 * 创建原因：建立可追踪的故障留痕通道，便于线上问题回溯。
 */
@Mapper
public interface ExceptionLogMapper extends BaseMapper<ExceptionLog> {
}

package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异常日志实体（对应 exception_log 表）。
 * 功能：记录系统告警/异常类型、摘要与详情文本。
 * 创建原因：把运行期异常留痕到数据库，方便后台监控页面查询与排障追踪。
 */
@Data
@TableName("exception_log")
public class ExceptionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String message;
    private String detail;
    private LocalDateTime createTime;
}

package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。
 * 功能：提供用户数据的基础 CRUD，支撑注册、登录、资料与后台用户管理。
 * 创建原因：统一用户持久化访问入口，隔离 Service 与底层 SQL。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

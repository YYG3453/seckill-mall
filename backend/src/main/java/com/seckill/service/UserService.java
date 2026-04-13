package com.seckill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seckill.dto.SessionUser;
import com.seckill.dto.UserLoginRequest;
import com.seckill.dto.UserProfileUpdateRequest;
import com.seckill.dto.UserRegisterRequest;
import com.seckill.entity.User;
import com.seckill.exception.BusinessException;
import com.seckill.interceptor.LoginInterceptor;
import com.seckill.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

/**
 * 用户注册与认证：密码使用 BCrypt 哈希；登录成功后向 HttpSession 写入 {@link com.seckill.dto.SessionUser}，
 * 键名为 {@link com.seckill.interceptor.LoginInterceptor#SESSION_USER_KEY}，供后续接口取当前用户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 用户名唯一校验通过后插入用户，默认角色 user、状态启用。 */
    public void register(UserRegisterRequest req) {
        Long c = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, req.getUsername()));
        if (c != null && c > 0) {
            throw new BusinessException("用户名已存在");
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setPhone(req.getPhone());
        u.setRole("user");
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        userMapper.insert(u);
        log.info("user registered username={}", u.getUsername());
    }

    /** 校验账号密码与禁用状态；成功则创建 Session（若容器需要）并写入 SessionUser。 */
    public SessionUser login(UserLoginRequest req, HttpSession session) {
        User u = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, req.getUsername()));
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BusinessException("账号已禁用");
        }
        SessionUser su = new SessionUser(u.getId(), u.getUsername(), u.getRole());
        session.setAttribute(LoginInterceptor.SESSION_USER_KEY, su);
        log.info("user login userId={}", u.getId());
        return su;
    }

    /** 使 Session 失效，清除服务端登录态（Spring Session Redis 会同步删 key）。 */
    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    public User profile(Long userId) {
        return userMapper.selectById(userId);
    }

    public void updateProfile(Long userId, UserProfileUpdateRequest req) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        if (req.getPhone() != null) {
            u.setPhone(req.getPhone());
        }
        if (req.getAvatar() != null) {
            String a = req.getAvatar().trim();
            u.setAvatar(a.isEmpty() ? null : a);
        }
        userMapper.updateById(u);
    }

    public void updateAvatar(Long userId, String url) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setAvatar(url);
        userMapper.updateById(u);
    }
}

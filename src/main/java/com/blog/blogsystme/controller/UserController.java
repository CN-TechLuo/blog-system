package com.blog.blogsystme.controller;

import com.blog.blogsystme.util.JwtUtil;
import com.blog.blogsystme.util.PasswordUtil;
import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.LoginRequest;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")

public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserMapper userMapper;

    /**
     * 注册接口：POST /api/user/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(@Valid @RequestBody User user) {

        log.debug("收到注册请求: username={}", user.getUsername());

        // 1. 检查用户名是否已存在
        User existUser = userMapper.findByUsername(user.getUsername());

        if (existUser != null) {
            log.info("注册失败: 用户名 {} 已存在", user.getUsername());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("用户名已存在，注册失败"));
        }

        // 2. 密码 BCrypt 加密
        String encodedPassword = PasswordUtil.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 3. 插入数据库
        int rows = userMapper.insert(user);

        log.info("注册结果: username={}, rows={}", user.getUsername(), rows);

        if (rows > 0) {
            return ResponseEntity.ok(ApiResponse.success("注册成功"));
        } else {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("注册失败，请稍后重试"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.debug("收到登录请求: username={}", loginRequest.getUsername());

        // 1. 根据用户名查询用户
        User user = userMapper.findByUsername(loginRequest.getUsername());
        if (user == null) {
            log.info("登录失败: 用户 {} 不存在", loginRequest.getUsername());
            return ResponseEntity.badRequest().body(ApiResponse.fail("用户不存在"));
        }

        // 2. 验证密码（Bcrypt matches）
        boolean matched = PasswordUtil.matches(loginRequest.getPassword(), user.getPassword());
        if (!matched) {
            log.info("登录失败: 用户名 {} 密码错误", loginRequest.getUsername());
            return ResponseEntity.badRequest().body(ApiResponse.fail("密码错误"));
        }

        // 3. 生成 JWT token
        String token = JwtUtil.generateToken(user.getUsername());

        log.info("登录成功: username={}", user.getUsername());

        // 4. 返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        data.put("userId", user.getId());
        return ResponseEntity.ok(ApiResponse.success("登录成功", data));
    }

}
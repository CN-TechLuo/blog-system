package com.blog.blogsystme.controller;

import com.blog.blogsystme.Util.JwtUtil;
import com.blog.blogsystme.Util.PasswordUtil;
import com.blog.blogsystme.dto.LoginRequest;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")  // 这个控制器的所有接口都以 /api/user 开头

public class UserController {

    @Autowired
    private UserMapper userMapper;  // 注入UserMapper，用来操作数据库

    // 注册接口：POST /api/user/register
    // 请求体格式：JSON {"username":"张三","password":"123456","email":"zhang@example.com"}
    @PostMapping("/register")
    public RegisterResponse register(@RequestBody User user) {

        System.out.println("接到注册请求:" + user.getUsername());

        // 1. 检查用户名是否已存在
        User existUser = userMapper.findByUsername(user.getUsername());

        System.out.println("查询结果:" + existUser);

        if (existUser != null) {
            return new RegisterResponse(false,"用户名已存在，注册失败") ;
        }
        //密码加密
        String encodedPassword = PasswordUtil.encode(user.getPassword());
        user.setPassword(encodedPassword);//加密后的密码返回user对象
        // 2. 密码加密
        // 3. 插入数据库
        int rows = userMapper.insert(user);

        System.out.println("插入影响行数:" + rows);

        if (rows > 0) {

            return new RegisterResponse(true,"注册成功") ;
        } else {
            return new RegisterResponse(false,"注册失败，请稍后重试");
    }

    }
    class RegisterResponse{
        private boolean success;    //是否成功
        private String message; //提示信息

        //构造方法
        public RegisterResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        //getter和setter(转成JSON）
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // 1. 根据用户名查询用户
        User user = userMapper.findByUsername(loginRequest.getUsername());
        if (user == null) {
            return ResponseEntity.badRequest().body(new RegisterResponse(false, "用户不存在"));
        }

        // 2. 验证密码（用 Bcrypt 的 matches 方法）
        boolean matched = PasswordUtil.matches(loginRequest.getPassword(), user.getPassword());
        if (!matched) {
            return ResponseEntity.badRequest().body(new RegisterResponse(false, "密码错误"));
        }

        // 3. 生成 JWT token
        String token = JwtUtil.generateToken(user.getUsername());

        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "登录成功");
        result.put("token", token);
        result.put("username", user.getUsername());

        return ResponseEntity.ok(result);
    }

}
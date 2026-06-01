package com.blog.blogsystme.controller;

import com.blog.blogsystme.Util.JwtUtil;
import com.blog.blogsystme.entity.Article;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.ArticleMapper;
import com.blog.blogsystme.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

    @RestController
    @RequestMapping("/api/article")
    public class ArticleController {

        @Autowired
        private ArticleMapper articleMapper;
        @Autowired
        private UserMapper userMapper;

        @PostMapping("/create")
        public String create(@RequestBody Article article, HttpServletRequest request) {
            // 1. 从请求头获取 token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "未登录或 token 格式错误";
            }
            String token = authHeader.substring(7);
            // 2. 解析 token 获取用户名
            String username = JwtUtil.getUsernameFromToken(token);
            if (username == null) {
                return "token 无效或已过期";
            }
            // 3. 查询用户得到 ID
            User user = userMapper.findByUsername(username);
            if (user == null) {
                return "用户不存在";
            }
            // 4. 设置作者 ID 并保存
            article.setAuthorId(user.getId());
            int rows = articleMapper.insert(article);
            return rows > 0 ? "发布成功，文章 ID=" + article.getId() : "发布失败";
        }
    }


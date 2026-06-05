package com.blog.blogsystme.controller;

import com.blog.blogsystme.Util.JwtUtil;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.entity.Article;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.ArticleMapper;
import com.blog.blogsystme.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.util.List;

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
        article.setUserId(user.getId());
        int rows = articleMapper.insert(article);
        return rows > 0 ? "发布成功，文章 ID=" + article.getId() : "发布失败";
    }

    @GetMapping("/list")
    public PageResponse list(@RequestParam (defaultValue = "1")int page,
                             @RequestParam (defaultValue = "10") int pageSize) {
        if (page < 1)
            page = 1;

        if (pageSize < 1)
            pageSize = 10;

        if (pageSize > 100)
            pageSize = 100;
        int start = (page - 1) * pageSize;
        List<Article>articles = articleMapper.findByPage(start, pageSize);
        int total =  articleMapper.count();
        return new PageResponse(true,"查询成功",articles,total,page,pageSize);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Integer id) {
        Article article = articleMapper.findById(id);
        if (article == null) {
            // 直接返回 404 状态码 + 错误信息，
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("文章不存在");
        }
        return ResponseEntity.ok(article);
    }
    public Integer getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        String username = JwtUtil.getUsernameFromToken(token);
        if (username == null) {
            return null;
        }
        User user = userMapper.findByUsername(username);
        return user != null ? user.getId() : null;
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody Article article, HttpServletRequest request) {

        //获取当前登入用户Id
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登入或 token无效");
        }

        //查询原文章
        Article existingArticle = articleMapper.findById(article.getId());
        if (existingArticle == null) {
            return ResponseEntity.badRequest().body("文章不存在");
        }

        //验证作者身份(仅作者可编辑)
        if (!existingArticle.getUserId().equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限编辑此文件");
        }

        //更近文章(仅更新标题和文章)
        existingArticle.setTitle(article.getTitle());
        existingArticle.setContent(article.getContent());
        int rows = articleMapper.update(existingArticle);
        if (rows > 0) {
            return ResponseEntity.ok("编辑成功");
        } else {
            return ResponseEntity.badRequest().body("编辑失败");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id, HttpServletRequest request) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登入或 token 无效");
            }

            //查询原文章
            Article article = articleMapper.findById(id);
            if (article == null) {
                return ResponseEntity.badRequest().body("文章不存在");
            }

            //验证作者身份
            if (!article.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限删除此文章");
            }

            //删除
            int rows = articleMapper.deleteById(id);
                    if(rows > 0) {
                        return ResponseEntity.ok("删除成功");
                    }else{
                        return ResponseEntity.badRequest().body("删除失败");
                    }
        }

    }

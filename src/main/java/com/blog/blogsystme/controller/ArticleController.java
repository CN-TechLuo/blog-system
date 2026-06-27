package com.blog.blogsystme.controller;

import com.blog.blogsystme.util.JwtUtil;
import com.blog.blogsystme.util.XssUtil;
import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.entity.Article;
import com.blog.blogsystme.mapper.ArticleMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/article")
public class ArticleController {

    private static final Logger log = LoggerFactory.getLogger(ArticleController.class);

    @Autowired
    private ArticleMapper articleMapper;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody Article article, HttpServletRequest request) {
        // 1. 获取当前登录用户 ID（统一认证逻辑）
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("未登录或 token 无效"));
        }

        // 2. 设置作者 ID、XSS 过滤并保存
        article.setUserId(currentUserId);
        article.setTitle(XssUtil.escape(article.getTitle()));
        article.setContent(XssUtil.escape(article.getContent()));
        int rows = articleMapper.insert(article);

        if (rows > 0) {
            log.info("文章发布成功: id={}, title={}, userId={}", article.getId(), article.getTitle(), currentUserId);
            return ResponseEntity.ok(ApiResponse.success("发布成功", (long) article.getId()));
        } else {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("发布失败"));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResponse>> list(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        int start = (page - 1) * pageSize;
        List<Article> articles = articleMapper.findByPage(start, pageSize);
        int total = articleMapper.count();
        return ResponseEntity.ok(ApiResponse.success("查询成功",
                new PageResponse(true, "查询成功", articles, total, pageSize, page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Article>> detail(@PathVariable Integer id) {
        Article article = articleMapper.findById(id);
        if (article == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("文章不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success("查询成功", article));
    }

    private Integer getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        // 直接从 JWT claims 获取 userId（getUserIdFromToken 内置异常处理，无效 token 返回 null）
        return JwtUtil.getUserIdFromToken(token);
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<Void>> update(@Valid @RequestBody Article article, HttpServletRequest request) {
        // 获取当前登录用户 ID
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("未登录或 token 无效"));
        }

        // 设置作者 ID、XSS 过滤，由 SQL 层鉴权（WHERE id = #{id} AND user_id = #{userId}）
        article.setUserId(currentUserId);
        article.setTitle(XssUtil.escape(article.getTitle()));
        article.setContent(XssUtil.escape(article.getContent()));
        int rows = articleMapper.updateByAuthor(article);

        if (rows > 0) {
            log.info("文章编辑成功: id={}", article.getId());
            return ResponseEntity.ok(ApiResponse.success("编辑成功"));
        } else {
            // rows=0 说明文章不存在或无权限
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("文章不存在或无权限编辑"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id, HttpServletRequest request) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("未登录或 token 无效"));
        }

        // SQL 层鉴权删除（WHERE id = #{id} AND user_id = #{userId}）
        int rows = articleMapper.deleteByIdAndAuthor(id, currentUserId);
        if (rows > 0) {
            log.info("文章删除成功: id={}", id);
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        } else {
            // rows=0 说明文章不存在或无权限
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("文章不存在或无权限删除"));
        }
    }

}

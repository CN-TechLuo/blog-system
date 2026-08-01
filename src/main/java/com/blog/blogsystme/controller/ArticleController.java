package com.blog.blogsystme.controller;

import com.blog.blogsystme.dto.ArticleCreateRequest;
import com.blog.blogsystme.dto.ArticleUpdateRequest;
import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.entity.Article;
import com.blog.blogsystme.mapper.ArticleMapper;
import com.blog.blogsystme.util.XssUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/article")
public class ArticleController {

    private static final Logger log = LoggerFactory.getLogger(ArticleController.class);

    @Autowired
    private ArticleMapper articleMapper;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody ArticleCreateRequest request,
                                                    HttpServletRequest httpRequest) {
        Integer currentUserId = (Integer) httpRequest.getAttribute("userId");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("未登录或 token 无效"));
        }

        Article article = new Article();
        article.setTitle(XssUtil.escape(request.getTitle()));
        article.setContent(XssUtil.sanitizeHtml(request.getContent()));
        article.setUserId(currentUserId);
        int rows = articleMapper.insert(article);

        if (rows > 0) {
            log.info("文章发布成功: id={}, title={}, userId={}", article.getId(), request.getTitle(), currentUserId);
            return ResponseEntity.ok(ApiResponse.success("发布成功", (long) article.getId()));
        }
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("发布失败"));
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
                new PageResponse(articles, total, pageSize, page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Article>> detail(@PathVariable Integer id) {
        Article article = articleMapper.findById(id);
        if (article == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("文章不存在"));
        }
        articleMapper.incrementViewCount(id);
        article.setViewCount(article.getViewCount() == null ? 1 : article.getViewCount() + 1);
        return ResponseEntity.ok(ApiResponse.success("查询成功", article));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<Void>> update(@Valid @RequestBody ArticleUpdateRequest request,
                                                    HttpServletRequest httpRequest) {
        Integer currentUserId = (Integer) httpRequest.getAttribute("userId");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("未登录或 token 无效"));
        }

        Article article = new Article();
        article.setId(request.getId());
        article.setTitle(XssUtil.escape(request.getTitle()));
        article.setContent(XssUtil.sanitizeHtml(request.getContent()));
        article.setUserId(currentUserId);
        int rows = articleMapper.updateByAuthor(article);

        if (rows > 0) {
            log.info("文章编辑成功: id={}", request.getId());
            return ResponseEntity.ok(ApiResponse.success("编辑成功"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("文章不存在或无权限编辑"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id,
                                                    HttpServletRequest httpRequest) {
        Integer currentUserId = (Integer) httpRequest.getAttribute("userId");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("未登录或 token 无效"));
        }

        int rows = articleMapper.deleteByIdAndAuthor(id, currentUserId);
        if (rows > 0) {
            log.info("文章删除成功: id={}", id);
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("文章不存在或无权限删除"));
    }

}

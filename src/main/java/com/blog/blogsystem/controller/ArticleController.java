package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ArticleCreateRequest;
import com.blog.blogsystem.dto.ArticleUpdateRequest;
import com.blog.blogsystem.dto.PageResponse;
import com.blog.blogsystem.entity.Article;
import com.blog.blogsystem.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/article")
@Tag(name = "文章管理", description = "文章 CRUD + 搜索")
public class ArticleController {

    private static final Logger log = LoggerFactory.getLogger(ArticleController.class);

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/create")
    @Operation(summary = "发布文章")
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody ArticleCreateRequest request,
                                                    @RequestAttribute("userId") Integer userId) {
        ApiResponse<Long> result = articleService.create(request, userId);
        return ResponseEntity.status(result.isSuccess() ? 200 : 400).body(result);
    }

    @GetMapping("/list")
    @Operation(summary = "文章列表（分页）")
    public ResponseEntity<ApiResponse<PageResponse>> list(HttpServletRequest request,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        Integer viewerId = (Integer) request.getAttribute("userId");
        return ResponseEntity.ok(articleService.list(page, pageSize, viewerId));
    }

    @GetMapping("/search")
    @Operation(summary = "文章搜索（按标题）")
    public ResponseEntity<ApiResponse<PageResponse>> search(HttpServletRequest request,
                                                            @RequestParam String keyword,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int pageSize) {
        Integer viewerId = (Integer) request.getAttribute("userId");
        return ResponseEntity.ok(articleService.search(keyword, page, pageSize, viewerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "文章详情")
    public ResponseEntity<ApiResponse<Article>> detail(HttpServletRequest request, @PathVariable Integer id) {
        Integer viewerId = (Integer) request.getAttribute("userId");
        ApiResponse<Article> result = articleService.detail(id, viewerId, request.getRemoteAddr());
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update")
    @Operation(summary = "编辑文章")
    public ResponseEntity<ApiResponse<Void>> update(@Valid @RequestBody ArticleUpdateRequest request,
                                                    @RequestAttribute("userId") Integer userId) {
        ApiResponse<Void> result = articleService.update(request, userId);
        return ResponseEntity.status(result.isSuccess() ? 200 : 403).body(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文章")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id,
                                                    @RequestAttribute("userId") Integer userId) {
        ApiResponse<Void> result = articleService.delete(id, userId);
        return ResponseEntity.status(result.isSuccess() ? 200 : 403).body(result);
    }

    @GetMapping("/feed/following")
    @Operation(summary = "关注流（需登录）")
    public ResponseEntity<ApiResponse<PageResponse>> followingFeed(HttpServletRequest request,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "10") int pageSize) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("请先登录"));
        }
        return ResponseEntity.ok(articleService.followingFeed(userId, page, pageSize));
    }

    @GetMapping("/feed/hot")
    @Operation(summary = "热门流（公开）")
    public ResponseEntity<ApiResponse<PageResponse>> hotFeed(HttpServletRequest request,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int pageSize) {
        Integer viewerId = (Integer) request.getAttribute("userId");
        return ResponseEntity.ok(articleService.hotFeed(page, pageSize, viewerId));
    }

}

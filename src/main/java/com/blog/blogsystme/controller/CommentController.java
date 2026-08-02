package com.blog.blogsystme.controller;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.CommentCreateRequest;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "评论管理", description = "文章评论的查看、发表、删除")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/api/article/{articleId}/comments")
    @Operation(summary = "查看文章评论")
    public ResponseEntity<ApiResponse<PageResponse>> list(@PathVariable Integer articleId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(commentService.list(articleId, page, pageSize));
    }

    @PostMapping("/api/article/{articleId}/comments")
    @Operation(summary = "发表评论")
    public ResponseEntity<ApiResponse<Long>> create(@PathVariable Integer articleId,
                                                    @RequestAttribute("userId") Integer userId,
                                                    @Valid @RequestBody CommentCreateRequest request) {
        ApiResponse<Long> result = commentService.create(articleId, userId, request);
        return ResponseEntity.status(result.isSuccess() ? 200 : 400).body(result);
    }

    @DeleteMapping("/api/comment/{id}")
    @Operation(summary = "删除评论")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id,
                                                    @RequestAttribute("userId") Integer userId) {
        ApiResponse<Void> result = commentService.delete(id, userId);
        return ResponseEntity.status(result.isSuccess() ? 200 : 403).body(result);
    }

}

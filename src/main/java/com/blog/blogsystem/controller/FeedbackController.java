package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.FeedbackCreateRequest;
import com.blog.blogsystem.dto.PageResponse;
import com.blog.blogsystem.entity.Feedback;
import com.blog.blogsystem.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "用户反馈", description = "提交反馈、查看反馈历史")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @Operation(summary = "提交反馈")
    public ResponseEntity<ApiResponse<Long>> create(@RequestAttribute("userId") Integer userId,
                                                    @Valid @RequestBody FeedbackCreateRequest request) {
        ApiResponse<Long> result = feedbackService.create(userId, request);
        return ResponseEntity.status(result.isSuccess() ? 200 : 400).body(result);
    }

    @GetMapping
    @Operation(summary = "我的反馈列表")
    public ResponseEntity<ApiResponse<PageResponse>> list(@RequestAttribute("userId") Integer userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(feedbackService.list(userId, page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "反馈详情")
    public ResponseEntity<ApiResponse<Feedback>> detail(@RequestAttribute("userId") Integer userId,
                                                        @PathVariable Integer id) {
        ApiResponse<Feedback> result = feedbackService.detail(id, userId);
        return ResponseEntity.status(result.isSuccess() ? 200 : 404).body(result);
    }

}

package com.blog.blogsystme.controller;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.mapper.NotificationMapper;
import com.blog.blogsystme.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    public NotificationController(NotificationService notificationService, NotificationMapper notificationMapper) {
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping
    public ApiResponse<PageResponse> list(HttpServletRequest request,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        Integer userId = (Integer) request.getAttribute("userId");
        return notificationService.list(userId, page, pageSize);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return notificationService.countUnread(userId);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return notificationService.markAllRead(userId);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(HttpServletRequest request, @PathVariable Integer id) {
        Integer userId = (Integer) request.getAttribute("userId");
        notificationMapper.markRead(id, userId);
        return ApiResponse.success("已读");
    }
}

package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.PageResponse;

public interface NotificationService {

    ApiResponse<PageResponse> list(Integer userId, int page, int pageSize);

    ApiResponse<Integer> countUnread(Integer userId);

    ApiResponse<Void> markAllRead(Integer userId);

    void create(Integer userId, String type, Integer fromUserId, Integer articleId, Integer commentId, String content);
}

package com.blog.blogsystme.service;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.PageResponse;

public interface NotificationService {

    ApiResponse<PageResponse> list(Integer userId, int page, int pageSize);

    ApiResponse<Integer> countUnread(Integer userId);

    ApiResponse<Void> markAllRead(Integer userId);

    void create(Integer userId, String type, Integer fromUserId, Integer articleId, Integer commentId, String content);
}

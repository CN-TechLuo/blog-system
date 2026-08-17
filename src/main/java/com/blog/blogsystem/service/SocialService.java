package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.PageResponse;

public interface SocialService {

    ApiResponse<Boolean> like(Integer userId, Integer articleId);
    ApiResponse<Boolean> unlike(Integer userId, Integer articleId);

    ApiResponse<Boolean> bookmark(Integer userId, Integer articleId);
    ApiResponse<Boolean> unbookmark(Integer userId, Integer articleId);

    ApiResponse<Boolean> follow(Integer followerId, Integer followeeId);
    ApiResponse<Boolean> unfollow(Integer followerId, Integer followeeId);

    ApiResponse<PageResponse> getBookmarks(Integer userId, int page, int pageSize);
}

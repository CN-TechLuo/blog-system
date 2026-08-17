package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.CommentCreateRequest;
import com.blog.blogsystem.dto.PageResponse;

public interface CommentService {

    ApiResponse<PageResponse> list(Integer articleId, int page, int pageSize);

    ApiResponse<Long> create(Integer articleId, Integer userId, CommentCreateRequest request);

    ApiResponse<Void> delete(Integer commentId, Integer userId);

}

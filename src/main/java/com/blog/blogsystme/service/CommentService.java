package com.blog.blogsystme.service;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.CommentCreateRequest;
import com.blog.blogsystme.dto.PageResponse;

public interface CommentService {

    ApiResponse<PageResponse> list(Integer articleId, int page, int pageSize);

    ApiResponse<Long> create(Integer articleId, Integer userId, CommentCreateRequest request);

    ApiResponse<Void> delete(Integer commentId, Integer userId);

}

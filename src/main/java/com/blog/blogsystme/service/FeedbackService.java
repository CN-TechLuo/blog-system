package com.blog.blogsystme.service;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.FeedbackCreateRequest;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.entity.Feedback;

public interface FeedbackService {

    ApiResponse<Long> create(Integer userId, FeedbackCreateRequest request);

    ApiResponse<PageResponse> list(Integer userId, int page, int pageSize);

    ApiResponse<Feedback> detail(Integer id, Integer userId);

}

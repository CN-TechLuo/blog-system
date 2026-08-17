package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.FeedbackCreateRequest;
import com.blog.blogsystem.dto.PageResponse;
import com.blog.blogsystem.entity.Feedback;

public interface FeedbackService {

    ApiResponse<Long> create(Integer userId, FeedbackCreateRequest request);

    ApiResponse<PageResponse> list(Integer userId, int page, int pageSize);

    ApiResponse<Feedback> detail(Integer id, Integer userId);

}

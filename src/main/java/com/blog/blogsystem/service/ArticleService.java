package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ArticleCreateRequest;
import com.blog.blogsystem.dto.ArticleUpdateRequest;
import com.blog.blogsystem.dto.PageResponse;
import com.blog.blogsystem.entity.Article;

public interface ArticleService {

    ApiResponse<Long> create(ArticleCreateRequest request, Integer userId);

    ApiResponse<PageResponse> list(int page, int pageSize, Integer viewerId);

    ApiResponse<Article> detail(Integer id, Integer viewerId, String clientKey);

    ApiResponse<Void> update(ArticleUpdateRequest request, Integer userId);

    ApiResponse<Void> delete(Integer id, Integer userId);

    ApiResponse<PageResponse> search(String keyword, int page, int pageSize, Integer viewerId);

    ApiResponse<PageResponse> followingFeed(Integer userId, int page, int pageSize);

    ApiResponse<PageResponse> hotFeed(int page, int pageSize, Integer viewerId);

}

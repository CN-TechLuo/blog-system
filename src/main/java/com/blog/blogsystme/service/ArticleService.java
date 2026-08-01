package com.blog.blogsystme.service;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.ArticleCreateRequest;
import com.blog.blogsystme.dto.ArticleUpdateRequest;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.entity.Article;

public interface ArticleService {

    ApiResponse<Long> create(ArticleCreateRequest request, Integer userId);

    ApiResponse<PageResponse> list(int page, int pageSize);

    ApiResponse<Article> detail(Integer id);

    ApiResponse<Void> update(ArticleUpdateRequest request, Integer userId);

    ApiResponse<Void> delete(Integer id, Integer userId);

    ApiResponse<PageResponse> search(String keyword, int page, int pageSize);

}

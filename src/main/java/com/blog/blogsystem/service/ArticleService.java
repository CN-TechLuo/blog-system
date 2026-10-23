package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ArticleCreateRequest;
import com.blog.blogsystem.dto.ArticleUpdateRequest;
import com.blog.blogsystem.dto.PageResponse;
import com.blog.blogsystem.entity.Article;

/**
 * 文章业务服务：发布/编辑/删除/列表/搜索/热榜
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public interface ArticleService {

    /**
     * 发布文章（含敏感词审核与 XSS 清洗）
     *
     * @param request 文章发布请求
     * @param userId  作者用户ID
     * @return 统一响应（data 为新文章ID）
     */
    ApiResponse<Long> create(ArticleCreateRequest request, Integer userId);

    /**
     * 分页查询文章列表（含当前用户点赞/收藏状态标记）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param viewerId 当前用户ID（未登录为 null）
     * @return 统一响应（data 为分页数据）
     */
    ApiResponse<PageResponse> list(int page, int pageSize, Integer viewerId);

    /**
     * 查询文章详情（含浏览量节流与点赞/收藏/关注状态）
     *
     * @param id        文章ID
     * @param viewerId  当前用户ID（未登录为 null）
     * @param clientKey 客户端标识（浏览量节流键）
     * @return 统一响应（data 为文章详情）
     */
    ApiResponse<Article> detail(Integer id, Integer viewerId, String clientKey);

    /**
     * 编辑文章（校验作者归属）
     *
     * @param request 文章编辑请求
     * @param userId  作者用户ID
     * @return 统一响应
     */
    ApiResponse<Void> update(ArticleUpdateRequest request, Integer userId);

    /**
     * 删除文章（校验作者归属）
     *
     * @param id     文章ID
     * @param userId 作者用户ID
     * @return 统一响应
     */
    ApiResponse<Void> delete(Integer id, Integer userId);

    /**
     * 按关键词搜索文章（优先全文索引，失败回退 LIKE）
     *
     * @param keyword  关键词
     * @param page     页码
     * @param pageSize 每页条数
     * @param viewerId 当前用户ID（未登录为 null）
     * @return 统一响应（data 为分页数据）
     */
    ApiResponse<PageResponse> search(String keyword, int page, int pageSize, Integer viewerId);

    /**
     * 分页查询关注用户的文章（关注流）
     *
     * @param userId   当前用户ID
     * @param page     页码
     * @param pageSize 每页条数
     * @return 统一响应（data 为分页数据）
     */
    ApiResponse<PageResponse> followingFeed(Integer userId, int page, int pageSize);

    /**
     * 分页查询热榜文章（按热度分排序）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param viewerId 当前用户ID（未登录为 null）
     * @return 统一响应（data 为分页数据）
     */
    ApiResponse<PageResponse> hotFeed(int page, int pageSize, Integer viewerId);

}

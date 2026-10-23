package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.PageResponse;

/**
 * 社交服务：关注/点赞/收藏与状态查询
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public interface SocialService {

    /**
     * 点赞文章（自动生成通知）
     *
     * @param userId    点赞用户ID
     * @param articleId 文章ID
     * @return 统一响应（data 为最新点赞状态）
     */
    ApiResponse<Boolean> like(Integer userId, Integer articleId);

    /**
     * 取消点赞
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 统一响应（data 为最新点赞状态）
     */
    ApiResponse<Boolean> unlike(Integer userId, Integer articleId);

    /**
     * 收藏文章
     *
     * @param userId    收藏用户ID
     * @param articleId 文章ID
     * @return 统一响应（data 为最新收藏状态）
     */
    ApiResponse<Boolean> bookmark(Integer userId, Integer articleId);

    /**
     * 取消收藏
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 统一响应（data 为最新收藏状态）
     */
    ApiResponse<Boolean> unbookmark(Integer userId, Integer articleId);

    /**
     * 关注用户（自动生成通知）
     *
     * @param followerId 关注者用户ID
     * @param followeeId 被关注者用户ID
     * @return 统一响应（data 为最新关注状态）
     */
    ApiResponse<Boolean> follow(Integer followerId, Integer followeeId);

    /**
     * 取消关注
     *
     * @param followerId 关注者用户ID
     * @param followeeId 被关注者用户ID
     * @return 统一响应（data 为最新关注状态）
     */
    ApiResponse<Boolean> unfollow(Integer followerId, Integer followeeId);

    /**
     * 分页查询用户的收藏列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页条数
     * @return 统一响应（data 为分页数据）
     */
    ApiResponse<PageResponse> getBookmarks(Integer userId, int page, int pageSize);

}

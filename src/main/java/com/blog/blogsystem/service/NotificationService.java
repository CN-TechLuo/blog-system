package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.PageResponse;

/**
 * 通知业务服务：列表/未读数/已读/聚合通知
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public interface NotificationService {

    /**
     * 分页查询用户的通知列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页条数
     * @return 统一响应（data 为分页数据）
     */
    ApiResponse<PageResponse> list(Integer userId, int page, int pageSize);

    /**
     * 查询未读通知数
     *
     * @param userId 用户ID
     * @return 统一响应（data 为未读数）
     */
    ApiResponse<Integer> countUnread(Integer userId);

    /**
     * 标记全部通知已读
     *
     * @param userId 用户ID
     * @return 统一响应
     */
    ApiResponse<Void> markAllRead(Integer userId);

    /**
     * 创建通知（点赞/评论/关注等互动触发，内部调用）
     *
     * @param userId      接收通知的用户ID
     * @param type        通知类型
     * @param fromUserId  操作发起者用户ID
     * @param articleId   关联文章ID（可为 null）
     * @param commentId   关联评论ID（可为 null）
     * @param content     通知内容
     */
    void create(Integer userId, String type, Integer fromUserId, Integer articleId, Integer commentId, String content);
}

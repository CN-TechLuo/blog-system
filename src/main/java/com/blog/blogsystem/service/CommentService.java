package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.CommentCreateRequest;
import com.blog.blogsystem.dto.PageResponse;

/**
 * 评论业务服务：发表/查询/删除（嵌套回复）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public interface CommentService {

    /**
     * 分页查询文章的评论（含嵌套回复组装）
     *
     * @param articleId 文章ID
     * @param page      页码
     * @param pageSize  每页条数
     * @return 统一响应（data 为分页数据）
     */
    ApiResponse<PageResponse> list(Integer articleId, int page, int pageSize);

    /**
     * 发表评论（含敏感词审核）
     *
     * @param articleId 文章ID
     * @param userId    评论用户ID
     * @param request   评论请求
     * @return 统一响应（data 为新评论ID）
     */
    ApiResponse<Long> create(Integer articleId, Integer userId, CommentCreateRequest request);

    /**
     * 删除评论（校验归属，子回复级联删除）
     *
     * @param commentId 评论ID
     * @param userId    评论者用户ID
     * @return 统一响应
     */
    ApiResponse<Void> delete(Integer commentId, Integer userId);

}

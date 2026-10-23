package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.FeedbackCreateRequest;
import com.blog.blogsystem.dto.PageResponse;
import com.blog.blogsystem.entity.Feedback;

/**
 * 反馈业务服务
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public interface FeedbackService {

    /**
     * 提交反馈（含敏感词审核）
     *
     * @param userId  反馈用户ID
     * @param request 反馈请求
     * @return 统一响应（data 为新反馈ID）
     */
    ApiResponse<Long> create(Integer userId, FeedbackCreateRequest request);

    /**
     * 分页查询自己的反馈列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页条数
     * @return 统一响应（data 为分页数据）
     */
    ApiResponse<PageResponse> list(Integer userId, int page, int pageSize);

    /**
     * 查询反馈详情（校验归属，防止越权）
     *
     * @param id     反馈ID
     * @param userId 用户ID
     * @return 统一响应（data 为反馈详情）
     */
    ApiResponse<Feedback> detail(Integer id, Integer userId);

}

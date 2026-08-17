package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.FeedbackCreateRequest;
import com.blog.blogsystem.dto.PageResponse;
import com.blog.blogsystem.entity.Feedback;
import com.blog.blogsystem.mapper.FeedbackMapper;
import com.blog.blogsystem.service.FeedbackService;
import com.blog.blogsystem.util.PageUtil;
import com.blog.blogsystem.util.XssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackServiceImpl.class);

    private final FeedbackMapper feedbackMapper;

    public FeedbackServiceImpl(FeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    @Override
    @Transactional
    public ApiResponse<Long> create(Integer userId, FeedbackCreateRequest request) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setTitle(XssUtil.escape(request.getTitle()));
        feedback.setContent(XssUtil.sanitizeHtml(request.getContent()));
        int rows = feedbackMapper.insert(feedback);

        if (rows > 0) {
            log.info("反馈提交成功: id={}, userId={}", feedback.getId(), userId);
            return ApiResponse.success("提交成功，感谢您的反馈", (long) feedback.getId());
        }
        return ApiResponse.fail("提交失败，请稍后重试");
    }

    @Override
    public ApiResponse<PageResponse> list(Integer userId, int page, int pageSize) {
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 50);
        int start = PageUtil.start(p, size);
        List<Feedback> list = feedbackMapper.findByUserId(userId, start, size);
        int total = feedbackMapper.countByUserId(userId);
        return ApiResponse.success("查询成功", new PageResponse(list, total, size, p));
    }

    @Override
    public ApiResponse<Feedback> detail(Integer id, Integer userId) {
        Feedback feedback = feedbackMapper.findByIdAndUser(id, userId);
        if (feedback == null) {
            return ApiResponse.fail("反馈不存在");
        }
        return ApiResponse.success("查询成功", feedback);
    }

}

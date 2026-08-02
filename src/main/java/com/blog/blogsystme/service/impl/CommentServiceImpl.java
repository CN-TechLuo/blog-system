package com.blog.blogsystme.service.impl;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.CommentCreateRequest;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.entity.Comment;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.CommentMapper;
import com.blog.blogsystme.mapper.UserMapper;
import com.blog.blogsystme.service.CommentService;
import com.blog.blogsystme.util.XssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    public CommentServiceImpl(CommentMapper commentMapper, UserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
    }

    @Override
    public ApiResponse<PageResponse> list(Integer articleId, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 50) pageSize = 50;

        int start = (page - 1) * pageSize;
        List<Comment> comments = commentMapper.findByArticleId(articleId, start, pageSize);
        populateUsernames(comments);
        int total = commentMapper.countByArticleId(articleId);
        return ApiResponse.success("查询成功", new PageResponse(comments, total, pageSize, page));
    }

    @Override
    @Transactional
    public ApiResponse<Long> create(Integer articleId, Integer userId, CommentCreateRequest request) {
        String safeContent = XssUtil.sanitizeHtml(request.getContent());
        if (safeContent.isBlank()) {
            return ApiResponse.fail("评论内容不能为空");
        }

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(safeContent);
        int rows = commentMapper.insert(comment);

        if (rows > 0) {
            log.info("评论发表成功: id={}, articleId={}, userId={}", comment.getId(), articleId, userId);
            return ApiResponse.success("评论成功", (long) comment.getId());
        }
        return ApiResponse.fail("评论失败");
    }

    @Override
    @Transactional
    public ApiResponse<Void> delete(Integer commentId, Integer userId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            return ApiResponse.fail("评论不存在");
        }

        int rows = commentMapper.deleteByIdAndUser(commentId, userId);
        if (rows > 0) {
            log.info("评论删除成功: id={}, userId={}", commentId, userId);
            return ApiResponse.success("删除成功");
        }
        return ApiResponse.fail("评论不存在或无权限删除");
    }

    private void populateUsernames(List<Comment> comments) {
        for (Comment comment : comments) {
            User user = userMapper.findById(comment.getUserId());
            if (user != null) {
                comment.setUsername(user.getUsername());
            }
        }
    }

}

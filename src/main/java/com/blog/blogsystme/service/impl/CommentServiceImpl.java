package com.blog.blogsystme.service.impl;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.CommentCreateRequest;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.entity.Comment;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.ArticleMapper;
import com.blog.blogsystme.mapper.CommentMapper;
import com.blog.blogsystme.mapper.UserMapper;
import com.blog.blogsystme.service.CommentService;
import com.blog.blogsystme.util.PageUtil;
import com.blog.blogsystme.util.XssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;

    public CommentServiceImpl(CommentMapper commentMapper, UserMapper userMapper, ArticleMapper articleMapper) {
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    public ApiResponse<PageResponse> list(Integer articleId, int page, int pageSize) {
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 50);
        int start = PageUtil.start(p, size);
        List<Comment> comments = commentMapper.findByArticleId(articleId, start, size);
        populateUsernames(comments);
        int total = commentMapper.countByArticleId(articleId);
        return ApiResponse.success("查询成功", new PageResponse(comments, total, size, p));
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
            articleMapper.incrementCommentCount(articleId);
            log.info("评论发表成功: id={}, articleId={}, userId={}", comment.getId(), articleId, userId);
            return ApiResponse.success("评论成功", (long) comment.getId());
        }
        return ApiResponse.fail("评论失败");
    }

    @Override
    @Transactional
    public ApiResponse<Void> delete(Integer commentId, Integer userId) {
        Comment comment = commentMapper.findByIdAndUser(commentId, userId);
        if (comment == null) {
            return ApiResponse.fail("评论不存在或无权限删除");
        }
        commentMapper.deleteByIdAndUser(commentId, userId);
        articleMapper.decrementCommentCount(comment.getArticleId());
        log.info("评论删除成功: id={}, userId={}", commentId, userId);
        return ApiResponse.success("删除成功");
    }

    private void populateUsernames(List<Comment> comments) {
        if (comments.isEmpty()) return;
        List<Integer> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, String> userMap = userMapper.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        for (Comment comment : comments) {
            String username = userMap.get(comment.getUserId());
            if (username != null) {
                comment.setUsername(username);
            }
        }
    }

}

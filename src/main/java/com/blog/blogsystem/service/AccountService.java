package com.blog.blogsystem.service;

import com.blog.blogsystem.entity.Article;
import com.blog.blogsystem.entity.Comment;
import com.blog.blogsystem.entity.Feedback;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.AiUsageMapper;
import com.blog.blogsystem.mapper.ArticleMapper;
import com.blog.blogsystem.mapper.BookmarkMapper;
import com.blog.blogsystem.mapper.CommentMapper;
import com.blog.blogsystem.mapper.FeedbackMapper;
import com.blog.blogsystem.mapper.FollowMapper;
import com.blog.blogsystem.mapper.LikeMapper;
import com.blog.blogsystem.mapper.NotificationMapper;
import com.blog.blogsystem.mapper.PasswordResetTokenMapper;
import com.blog.blogsystem.mapper.ReportMapper;
import com.blog.blogsystem.mapper.TagMapper;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.util.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账号注销（个保法删除权）与数据导出（个保法可携带权）：
 * - 注销：级联删除用户全部个人数据（文章/评论/点赞/收藏/关注/通知/反馈/举报/AI 用量等）
 * - 导出：返回用户档案与内容数据 JSON，由前端提供下载
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final int EXPORT_LIMIT = 200;

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final FeedbackMapper feedbackMapper;
    private final LikeMapper likeMapper;
    private final BookmarkMapper bookmarkMapper;
    private final FollowMapper followMapper;
    private final NotificationMapper notificationMapper;
    private final PasswordResetTokenMapper tokenMapper;
    private final ReportMapper reportMapper;
    private final TagMapper tagMapper;
    private final AiUsageMapper aiUsageMapper;

    public AccountService(UserMapper userMapper, ArticleMapper articleMapper,
                          CommentMapper commentMapper, FeedbackMapper feedbackMapper,
                          LikeMapper likeMapper, BookmarkMapper bookmarkMapper,
                          FollowMapper followMapper, NotificationMapper notificationMapper,
                          PasswordResetTokenMapper tokenMapper, ReportMapper reportMapper,
                          TagMapper tagMapper, AiUsageMapper aiUsageMapper) {
        this.userMapper = userMapper;
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.feedbackMapper = feedbackMapper;
        this.likeMapper = likeMapper;
        this.bookmarkMapper = bookmarkMapper;
        this.followMapper = followMapper;
        this.notificationMapper = notificationMapper;
        this.tokenMapper = tokenMapper;
        this.reportMapper = reportMapper;
        this.tagMapper = tagMapper;
        this.aiUsageMapper = aiUsageMapper;
    }

    @Transactional
    public void deleteAccount(Integer userId) {
        // 1. 举报记录（举报人维度）
        reportMapper.deleteByReporterId(userId);
        // 2. 通知（接收 + 发出的通知）
        notificationMapper.deleteByUserId(userId);
        // 3. 用户发表的评论（嵌套回复随父评论级联删除）
        commentMapper.deleteByUserId(userId);
        // 4. 用户文章及其关联数据（点赞/收藏/评论/通知/标签关联，FK 约束下必须先清理）
        List<Integer> articleIds = articleMapper.findIdsByUserId(userId);
        articleIds.forEach(id -> {
            likeMapper.deleteByArticleId(id);
            bookmarkMapper.deleteByArticleId(id);
            commentMapper.deleteByArticleId(id);
            notificationMapper.deleteByArticleId(id);
            tagMapper.deleteArticleTags(id);
        });
        articleMapper.deleteByUserId(userId);
        // 5. 社交关系
        likeMapper.deleteByUserId(userId);
        bookmarkMapper.deleteByUserId(userId);
        followMapper.deleteByUserId(userId);
        // 6. 反馈与密码重置令牌
        feedbackMapper.deleteByUserId(userId);
        tokenMapper.deleteByUserId(userId);
        // 7. AI 用量记录
        aiUsageMapper.deleteByUserId(userId);
        // 8. 用户本体
        userMapper.deleteById(userId);
        AuditLogger.log("ACCOUNT_DELETED", "userId=" + userId + ", articles=" + articleIds.size());
        log.info("账号注销完成: userId={}, 删除文章 {} 篇", userId, articleIds.size());
    }

    public Map<String, Object> exportData(Integer userId) {
        User user = userMapper.findById(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        if (user == null) {
            return data;
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("role", user.getRole());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("createTime", user.getCreateTime());
        data.put("profile", profile);
        data.put("articleCount", articleMapper.countByUserId(userId));
        data.put("articles", articleMapper.findByUserId(userId, EXPORT_LIMIT).stream()
                .map(this::articleSummary).collect(Collectors.toList()));
        data.put("comments", commentMapper.findByUserId(userId, EXPORT_LIMIT).stream()
                .map(this::commentSummary).collect(Collectors.toList()));
        data.put("feedback", feedbackMapper.findByUserId(userId, 0, EXPORT_LIMIT).stream()
                .map(this::feedbackSummary).collect(Collectors.toList()));
        data.put("followCount", followMapper.countFollowing(userId));
        data.put("followerCount", followMapper.countFollowers(userId));
        data.put("exportTime", java.time.LocalDateTime.now().toString());
        return data;
    }

    private Map<String, Object> articleSummary(Article a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("viewCount", a.getViewCount());
        m.put("likeCount", a.getLikeCount());
        m.put("commentCount", a.getCommentCount());
        m.put("aiGenerated", a.getAiGenerated());
        m.put("createTime", a.getCreateTime());
        return m;
    }

    private Map<String, Object> commentSummary(Comment c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("articleId", c.getArticleId());
        m.put("content", c.getContent());
        m.put("createTime", c.getCreateTime());
        return m;
    }

    private Map<String, Object> feedbackSummary(Feedback f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId());
        m.put("title", f.getTitle());
        m.put("content", f.getContent());
        m.put("status", f.getStatus());
        m.put("createTime", f.getCreateTime());
        return m;
    }

}

package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.PageResponse;
import com.blog.blogsystem.entity.ArticleLike;
import com.blog.blogsystem.entity.Bookmark;
import com.blog.blogsystem.entity.Follow;
import com.blog.blogsystem.mapper.ArticleMapper;
import com.blog.blogsystem.mapper.BookmarkMapper;
import com.blog.blogsystem.mapper.FollowMapper;
import com.blog.blogsystem.mapper.LikeMapper;
import com.blog.blogsystem.service.NotificationService;
import com.blog.blogsystem.service.SocialService;
import com.blog.blogsystem.util.PageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SocialServiceImpl implements SocialService {

    private static final Logger log = LoggerFactory.getLogger(SocialServiceImpl.class);

    private final LikeMapper likeMapper;
    private final BookmarkMapper bookmarkMapper;
    private final FollowMapper followMapper;
    private final ArticleMapper articleMapper;
    private final NotificationService notificationService;

    public SocialServiceImpl(LikeMapper likeMapper, BookmarkMapper bookmarkMapper,
                              FollowMapper followMapper, ArticleMapper articleMapper,
                              NotificationService notificationService) {
        this.likeMapper = likeMapper;
        this.bookmarkMapper = bookmarkMapper;
        this.followMapper = followMapper;
        this.articleMapper = articleMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public ApiResponse<Boolean> like(Integer userId, Integer articleId) {
        if (likeMapper.exists(userId, articleId) > 0) {
            return ApiResponse.fail("已点赞");
        }
        ArticleLike like = new ArticleLike();
        like.setUserId(userId);
        like.setArticleId(articleId);
        try {
            likeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            // 并发双击命中唯一索引，视为已点赞
            return ApiResponse.fail("已点赞");
        }
        articleMapper.incrementLikeCount(articleId);
        return ApiResponse.success("点赞成功", true);
    }

    @Override
    @Transactional
    public ApiResponse<Boolean> unlike(Integer userId, Integer articleId) {
        int removed = likeMapper.delete(userId, articleId);
        if (removed > 0) {
            articleMapper.decrementLikeCount(articleId);
        }
        return ApiResponse.success("取消点赞", false);
    }

    @Override
    @Transactional
    public ApiResponse<Boolean> bookmark(Integer userId, Integer articleId) {
        if (bookmarkMapper.exists(userId, articleId) > 0) {
            return ApiResponse.fail("已收藏");
        }
        Bookmark bm = new Bookmark();
        bm.setUserId(userId);
        bm.setArticleId(articleId);
        try {
            bookmarkMapper.insert(bm);
        } catch (DuplicateKeyException e) {
            return ApiResponse.fail("已收藏");
        }
        articleMapper.incrementBookmarkCount(articleId);
        return ApiResponse.success("收藏成功", true);
    }

    @Override
    @Transactional
    public ApiResponse<Boolean> unbookmark(Integer userId, Integer articleId) {
        int removed = bookmarkMapper.delete(userId, articleId);
        if (removed > 0) {
            articleMapper.decrementBookmarkCount(articleId);
        }
        return ApiResponse.success("取消收藏", false);
    }

    @Override
    @Transactional
    public ApiResponse<Boolean> follow(Integer followerId, Integer followeeId) {
        if (followerId.equals(followeeId)) {
            return ApiResponse.fail("不能关注自己");
        }
        if (followMapper.exists(followerId, followeeId) > 0) {
            return ApiResponse.fail("已关注");
        }
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            return ApiResponse.fail("已关注");
        }
        notificationService.create(followeeId, "follow", followerId, null, null, null);
        return ApiResponse.success("关注成功", true);
    }

    @Override
    @Transactional
    public ApiResponse<Boolean> unfollow(Integer followerId, Integer followeeId) {
        followMapper.delete(followerId, followeeId);
        return ApiResponse.success("取消关注", false);
    }

    @Override
    public ApiResponse<PageResponse> getBookmarks(Integer userId, int page, int pageSize) {
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 50);
        int start = PageUtil.start(p, size);
        List<Bookmark> list = bookmarkMapper.findByUserId(userId, start, size);
        int total = bookmarkMapper.countByUserId(userId);
        return ApiResponse.success("查询成功", new PageResponse(list, total, size, p));
    }
}

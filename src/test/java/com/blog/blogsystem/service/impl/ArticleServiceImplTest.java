package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ArticleCreateRequest;
import com.blog.blogsystem.entity.Article;
import com.blog.blogsystem.mapper.ArticleMapper;
import com.blog.blogsystem.mapper.BookmarkMapper;
import com.blog.blogsystem.mapper.CommentMapper;
import com.blog.blogsystem.mapper.LikeMapper;
import com.blog.blogsystem.mapper.NotificationMapper;
import com.blog.blogsystem.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArticleServiceImplTest {

    private final ArticleMapper articleMapper = mock(ArticleMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final LikeMapper likeMapper = mock(LikeMapper.class);
    private final BookmarkMapper bookmarkMapper = mock(BookmarkMapper.class);
    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final CacheManager cacheManager = mock(CacheManager.class);

    private final ArticleServiceImpl service = new ArticleServiceImpl(
            articleMapper, userMapper, commentMapper, likeMapper,
            bookmarkMapper, notificationMapper, cacheManager);

    @Test
    void createShouldRejectSensitiveContent() {
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("正常标题");
        request.setContent("这是包含赌博平台的内容");

        ApiResponse<Long> result = service.create(request, 1);
        assertFalse(result.isSuccess());
        assertEquals("内容包含违规词汇，请修改后重试", result.getMessage());
    }

    @Test
    void createShouldSetAiGeneratedFlag() {
        when(articleMapper.insert(any(Article.class))).thenAnswer(inv -> {
            inv.getArgument(0, Article.class).setId(42);
            return 1;
        });

        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("正常标题");
        request.setContent("正常内容");
        request.setAiGenerated(true);

        ApiResponse<Long> result = service.create(request, 1);
        assertTrue(result.isSuccess());

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        org.mockito.Mockito.verify(articleMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getAiGenerated());
    }

    @Test
    void createWithoutAiFlagDefaultsToZero() {
        when(articleMapper.insert(any(Article.class))).thenAnswer(inv -> {
            inv.getArgument(0, Article.class).setId(42);
            return 1;
        });

        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("正常标题");
        request.setContent("正常内容");
        request.setAiGenerated(null);

        ApiResponse<Long> result = service.create(request, 1);
        assertTrue(result.isSuccess());

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        org.mockito.Mockito.verify(articleMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getAiGenerated());
    }

}

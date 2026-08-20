package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ArticleCreateRequest;
import com.blog.blogsystem.dto.ArticleUpdateRequest;
import com.blog.blogsystem.dto.PageResponse;
import com.blog.blogsystem.entity.Article;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.ArticleMapper;
import com.blog.blogsystem.mapper.BookmarkMapper;
import com.blog.blogsystem.mapper.CommentMapper;
import com.blog.blogsystem.mapper.LikeMapper;
import com.blog.blogsystem.mapper.NotificationMapper;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.service.ArticleService;
import com.blog.blogsystem.util.PageUtil;
import com.blog.blogsystem.util.SensitiveWordFilter;
import com.blog.blogsystem.util.XssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    /** 浏览计数节流：同一访客对同一文章 10 分钟内只计一次，降低写放大 */
    private static final long VIEW_THROTTLE_MS = 10 * 60 * 1000L;
    private static final ConcurrentHashMap<String, Long> VIEW_THROTTLE = new ConcurrentHashMap<>();

    static {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "view-throttle-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            VIEW_THROTTLE.entrySet().removeIf(e -> now - e.getValue() > VIEW_THROTTLE_MS);
        }, 10, 10, TimeUnit.MINUTES);
    }

    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final LikeMapper likeMapper;
    private final BookmarkMapper bookmarkMapper;
    private final NotificationMapper notificationMapper;
    private final CacheManager cacheManager;

    public ArticleServiceImpl(ArticleMapper articleMapper, UserMapper userMapper,
                              CommentMapper commentMapper, LikeMapper likeMapper,
                              BookmarkMapper bookmarkMapper, NotificationMapper notificationMapper,
                              CacheManager cacheManager) {
        this.articleMapper = articleMapper;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.bookmarkMapper = bookmarkMapper;
        this.notificationMapper = notificationMapper;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public ApiResponse<Long> create(ArticleCreateRequest request, Integer userId) {
        if (SensitiveWordFilter.containsSensitive(request.getTitle())
                || SensitiveWordFilter.containsSensitive(request.getContent())) {
            log.warn("内容审核拦截: userId={}, title 命中敏感词", userId);
            return ApiResponse.fail("内容包含违规词汇，请修改后重试");
        }
        Article article = new Article();
        article.setTitle(XssUtil.escape(request.getTitle()));
        article.setContent(XssUtil.sanitizeHtml(request.getContent()));
        article.setUserId(userId);
        article.setCoverUrl(request.getCoverUrl());
        article.setAiGenerated(Boolean.TRUE.equals(request.getAiGenerated()) ? 1 : 0);
        int rows = articleMapper.insert(article);

        if (rows > 0) {
            evictHotFeedCache();
            log.info("文章发布成功: id={}, title={}, userId={}", article.getId(), request.getTitle(), userId);
            return ApiResponse.success("发布成功", (long) article.getId());
        }
        return ApiResponse.fail("发布失败");
    }

    @Override
    public ApiResponse<PageResponse> list(int page, int pageSize, Integer viewerId) {
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 100);
        int start = PageUtil.start(p, size);
        List<Article> articles = articleMapper.findByPage(start, size);
        populateAuthorNames(articles);
        populateSocialState(articles, viewerId);
        int total = articleMapper.count();
        return ApiResponse.success("查询成功", new PageResponse(articles, total, size, p));
    }

    @Override
    public ApiResponse<Article> detail(Integer id, Integer viewerId, String clientKey) {
        Article article = articleMapper.findById(id);
        if (article == null) {
            return ApiResponse.fail("文章不存在");
        }
        // 同一访客 10 分钟内重复浏览不重复计数
        String throttleKey = id + ":" + (viewerId != null ? "u" + viewerId : "ip" + clientKey);
        long now = System.currentTimeMillis();
        Long last = VIEW_THROTTLE.putIfAbsent(throttleKey, now);
        if (last == null || now - last > VIEW_THROTTLE_MS) {
            if (last != null) VIEW_THROTTLE.put(throttleKey, now);
            articleMapper.incrementViewCount(id);
            article.setViewCount(article.getViewCount() == null ? 1 : article.getViewCount() + 1);
        }

        User author = userMapper.findById(article.getUserId());
        if (author != null) {
            article.setAuthorName(author.getNickname() != null ? author.getNickname() : author.getUsername());
            article.setAuthorAvatar(author.getAvatarUrl());
        }
        if (viewerId != null) {
            article.setIsLiked(likeMapper.exists(viewerId, id) > 0);
            article.setIsBookmarked(bookmarkMapper.exists(viewerId, id) > 0);
        }
        return ApiResponse.success("查询成功", article);
    }

    @Override
    @Transactional
    public ApiResponse<Void> update(ArticleUpdateRequest request, Integer userId) {
        if (SensitiveWordFilter.containsSensitive(request.getTitle())
                || SensitiveWordFilter.containsSensitive(request.getContent())) {
            return ApiResponse.fail("内容包含违规词汇，请修改后重试");
        }
        Article article = new Article();
        article.setId(request.getId());
        article.setTitle(XssUtil.escape(request.getTitle()));
        article.setContent(XssUtil.sanitizeHtml(request.getContent()));
        article.setUserId(userId);
        article.setCoverUrl(request.getCoverUrl());
        article.setAiGenerated(Boolean.TRUE.equals(request.getAiGenerated()) ? 1 : 0);
        int rows = articleMapper.updateByAuthor(article);

        if (rows > 0) {
            evictHotFeedCache();
            log.info("文章编辑成功: id={}", request.getId());
            return ApiResponse.success("编辑成功");
        }
        return ApiResponse.fail("文章不存在或无权限编辑");
    }

    @Override
    @Transactional
    public ApiResponse<Void> delete(Integer id, Integer userId) {
        int rows = articleMapper.deleteByIdAndAuthor(id, userId);
        if (rows > 0) {
            likeMapper.deleteByArticleId(id);
            bookmarkMapper.deleteByArticleId(id);
            commentMapper.deleteByArticleId(id);
            notificationMapper.deleteByArticleId(id);
            evictHotFeedCache();
            log.info("文章删除成功: id={}", id);
            return ApiResponse.success("删除成功");
        }
        return ApiResponse.fail("文章不存在或无权限删除");
    }

    @Override
    public ApiResponse<PageResponse> search(String keyword, int page, int pageSize, Integer viewerId) {
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 100);
        int start = PageUtil.start(p, size);
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return ApiResponse.success("查询成功", new PageResponse(List.of(), 0, size, p));
        }
        // 长度 >= 2 使用 FULLTEXT(ngram) 索引搜索，短关键词回退 LIKE
        List<Article> articles = kw.length() >= 2
                ? articleMapper.searchByTitleFulltext(kw, start, size)
                : articleMapper.searchByTitle(kw, start, size);
        populateAuthorNames(articles);
        populateSocialState(articles, viewerId);
        int total = kw.length() >= 2
                ? articleMapper.countByKeywordFulltext(kw)
                : articleMapper.countByKeyword(kw);
        return ApiResponse.success("查询成功", new PageResponse(articles, total, size, p));
    }

    private void populateAuthorNames(List<Article> articles) {
        if (articles.isEmpty()) return;
        List<Integer> userIds = articles.stream()
                .map(Article::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, User> userMap = userMapper.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        articles.forEach(a -> {
            User user = userMap.get(a.getUserId());
            if (user != null) {
                a.setAuthorName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                a.setAuthorAvatar(user.getAvatarUrl());
            }
        });
    }

    @Override
    public ApiResponse<PageResponse> followingFeed(Integer userId, int page, int pageSize) {
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 50);
        int start = PageUtil.start(p, size);
        List<Article> articles = articleMapper.findFollowingFeed(userId, start, size);
        populateAuthorNames(articles);
        populateSocialState(articles, userId);
        int total = articleMapper.countFollowingFeed(userId);
        return ApiResponse.success("查询成功", new PageResponse(articles, total, size, p));
    }

    @Override
    public ApiResponse<PageResponse> hotFeed(int page, int pageSize, Integer viewerId) {
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 50);
        int start = PageUtil.start(p, size);

        // 热榜列表缓存 60 秒；社交状态（点赞/收藏）属 viewer 私有，不进缓存
        Cache cache = cacheManager.getCache("hotFeed");
        String cacheKey = p + ":" + size;
        @SuppressWarnings("unchecked")
        List<Article> articles = cache != null ? cache.get(cacheKey, List.class) : null;
        if (articles == null) {
            articles = articleMapper.findHotFeed(start, size);
            populateAuthorNames(articles);
            if (cache != null) cache.put(cacheKey, articles);
        }

        populateSocialState(articles, viewerId);
        int total = articleMapper.count();
        return ApiResponse.success("查询成功", new PageResponse(articles, total, size, p));
    }

    private void evictHotFeedCache() {
        try {
            Cache cache = cacheManager.getCache("hotFeed");
            if (cache != null) cache.clear();
        } catch (Exception ignored) {}
    }

    private void populateSocialState(List<Article> articles, Integer viewerId) {
        if (viewerId == null || articles.isEmpty()) return;
        List<Integer> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());
        try {
            var liked = likeMapper.findLikedArticleIds(viewerId, articleIds);
            var bookmarked = bookmarkMapper.findBookmarkedArticleIds(viewerId, articleIds);
            articles.forEach(a -> {
                a.setIsLiked(liked.contains(a.getId()));
                a.setIsBookmarked(bookmarked.contains(a.getId()));
            });
        } catch (Exception e) {
            log.warn("社交状态回显失败: viewerId={}", viewerId, e);
        }
    }

}

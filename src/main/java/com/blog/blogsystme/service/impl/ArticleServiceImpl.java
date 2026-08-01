package com.blog.blogsystme.service.impl;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.ArticleCreateRequest;
import com.blog.blogsystme.dto.ArticleUpdateRequest;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.entity.Article;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.ArticleMapper;
import com.blog.blogsystme.mapper.UserMapper;
import com.blog.blogsystme.service.ArticleService;
import com.blog.blogsystme.util.XssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;

    public ArticleServiceImpl(ArticleMapper articleMapper, UserMapper userMapper) {
        this.articleMapper = articleMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public ApiResponse<Long> create(ArticleCreateRequest request, Integer userId) {
        Article article = new Article();
        article.setTitle(XssUtil.escape(request.getTitle()));
        article.setContent(XssUtil.sanitizeHtml(request.getContent()));
        article.setUserId(userId);
        int rows = articleMapper.insert(article);

        if (rows > 0) {
            log.info("文章发布成功: id={}, title={}, userId={}", article.getId(), request.getTitle(), userId);
            return ApiResponse.success("发布成功", (long) article.getId());
        }
        return ApiResponse.fail("发布失败");
    }

    @Override
    public ApiResponse<PageResponse> list(int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        int start = (page - 1) * pageSize;
        List<Article> articles = articleMapper.findByPage(start, pageSize);
        populateAuthorNames(articles);
        int total = articleMapper.count();
        return ApiResponse.success("查询成功", new PageResponse(articles, total, pageSize, page));
    }

    @Override
    public ApiResponse<Article> detail(Integer id) {
        Article article = articleMapper.findById(id);
        if (article == null) {
            return ApiResponse.fail("文章不存在");
        }
        articleMapper.incrementViewCount(id);
        article.setViewCount(article.getViewCount() == null ? 1 : article.getViewCount() + 1);

        User author = userMapper.findById(article.getUserId());
        if (author != null) {
            article.setAuthorName(author.getUsername());
        }
        return ApiResponse.success("查询成功", article);
    }

    @Override
    @Transactional
    public ApiResponse<Void> update(ArticleUpdateRequest request, Integer userId) {
        Article article = new Article();
        article.setId(request.getId());
        article.setTitle(XssUtil.escape(request.getTitle()));
        article.setContent(XssUtil.sanitizeHtml(request.getContent()));
        article.setUserId(userId);
        int rows = articleMapper.updateByAuthor(article);

        if (rows > 0) {
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
            log.info("文章删除成功: id={}", id);
            return ApiResponse.success("删除成功");
        }
        return ApiResponse.fail("文章不存在或无权限删除");
    }

    @Override
    public ApiResponse<PageResponse> search(String keyword, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        int start = (page - 1) * pageSize;
        List<Article> articles = articleMapper.searchByTitle(keyword, start, pageSize);
        populateAuthorNames(articles);
        int total = articleMapper.countByKeyword(keyword);
        return ApiResponse.success("查询成功", new PageResponse(articles, total, pageSize, page));
    }

    private void populateAuthorNames(List<Article> articles) {
        for (Article article : articles) {
            if (article.getUserId() != null) {
                User author = userMapper.findById(article.getUserId());
                if (author != null) {
                    article.setAuthorName(author.getUsername());
                }
            }
        }
    }

}

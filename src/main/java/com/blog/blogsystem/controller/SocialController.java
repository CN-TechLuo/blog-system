package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.PageResponse;
import com.blog.blogsystem.service.SocialService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @PostMapping("/like/{articleId}")
    public ApiResponse<Boolean> like(HttpServletRequest request, @PathVariable Integer articleId) {
        Integer userId = (Integer) request.getAttribute("userId");
        return socialService.like(userId, articleId);
    }

    @DeleteMapping("/like/{articleId}")
    public ApiResponse<Boolean> unlike(HttpServletRequest request, @PathVariable Integer articleId) {
        Integer userId = (Integer) request.getAttribute("userId");
        return socialService.unlike(userId, articleId);
    }

    @PostMapping("/bookmark/{articleId}")
    public ApiResponse<Boolean> bookmark(HttpServletRequest request, @PathVariable Integer articleId) {
        Integer userId = (Integer) request.getAttribute("userId");
        return socialService.bookmark(userId, articleId);
    }

    @DeleteMapping("/bookmark/{articleId}")
    public ApiResponse<Boolean> unbookmark(HttpServletRequest request, @PathVariable Integer articleId) {
        Integer userId = (Integer) request.getAttribute("userId");
        return socialService.unbookmark(userId, articleId);
    }

    @PostMapping("/follow/{followeeId}")
    public ApiResponse<Boolean> follow(HttpServletRequest request, @PathVariable Integer followeeId) {
        Integer userId = (Integer) request.getAttribute("userId");
        return socialService.follow(userId, followeeId);
    }

    @DeleteMapping("/follow/{followeeId}")
    public ApiResponse<Boolean> unfollow(HttpServletRequest request, @PathVariable Integer followeeId) {
        Integer userId = (Integer) request.getAttribute("userId");
        return socialService.unfollow(userId, followeeId);
    }

    @GetMapping("/bookmarks")
    public ApiResponse<PageResponse> bookmarks(HttpServletRequest request,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        Integer userId = (Integer) request.getAttribute("userId");
        return socialService.getBookmarks(userId, page, pageSize);
    }
}

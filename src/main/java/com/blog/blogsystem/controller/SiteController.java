package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.service.SiteConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 站点公共配置接口（无需登录）
 */
@RestController
@RequestMapping("/api/site")
public class SiteController {

    private final SiteConfigService siteConfigService;

    public SiteController(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    @GetMapping("/contact-email")
    public ApiResponse<Map<String, Object>> contactEmail() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contactEmail", siteConfigService.getContactEmail());
        return ApiResponse.success("查询成功", data);
    }
}

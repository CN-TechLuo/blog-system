package com.blog.blogsystem.dto;

import lombok.Getter;

import java.util.List;

/**
 * 分页响应（被 ApiResponse 包裹，不重复携带 success/message）
 */
@Getter
public class PageResponse {

    private final List<?> data;
    private final int total;
    private final int pageSize;
    private final int page;

    public PageResponse(List<?> data, int total, int pageSize, int page) {
        this.data = data;
        this.total = total;
        this.pageSize = pageSize;
        this.page = page;
    }

}

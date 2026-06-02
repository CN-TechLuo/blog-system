package com.blog.blogsystme.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class PageResponse {
    private boolean success;
    private String message;
    private List<?> data;   //文章页表
    private int total;      //总记录数
    private int pageSize;   // 每页大小
    //getter和setter
    private int page;       //当前页面

    //构造方法
    public PageResponse(
            boolean success, String  message, List<?> data, int total, int pageSize, int page)
    {

        this.message=message;
        this.data=data;
        this.total=total;
        this.pageSize=pageSize;
        this.page = page;
        this.success = success;
    }

}

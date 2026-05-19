package com.blog.blogsystme;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/hello")
public String hello(){
        return "Hello World,博客系统正在建设中...";
    }
}

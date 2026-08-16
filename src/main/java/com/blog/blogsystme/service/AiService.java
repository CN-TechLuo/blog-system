package com.blog.blogsystme.service;

import com.blog.blogsystme.dto.ApiResponse;

import java.io.OutputStream;
import java.util.Map;

public interface AiService {

    ApiResponse<Map<String, Object>> generate(String prompt, String style);

    ApiResponse<Map<String, Object>> polish(String content);

    ApiResponse<Map<String, Object>> summarize(String content);

    ApiResponse<Map<String, Object>> expand(String content);

    ApiResponse<Map<String, Object>> generateTags(String content);

    ApiResponse<Map<String, Object>> chat(String message, String historyJson);

    /** 流式对话：将 DeepSeek 的增量内容以 data: 格式写入 out，结束时写 data: [DONE] */
    void streamChat(String message, String historyJson, OutputStream out) throws Exception;

    ApiResponse<Map<String, Object>> code(String prompt, String language);

    ApiResponse<Map<String, Object>> translate(String text, String targetLang);

    ApiResponse<Map<String, Object>> headline(String content);

    ApiResponse<Map<String, Object>> seo(String content);

    ApiResponse<Map<String, Object>> outline(String prompt);

    ApiResponse<Map<String, Object>> socialPost(String content);

    ApiResponse<Map<String, Object>> grammarFix(String content);
}

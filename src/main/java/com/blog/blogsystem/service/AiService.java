package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;

import java.io.OutputStream;
import java.util.Map;

/**
 * AI 能力服务：12 种创作模式 + 流式对话
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public interface AiService {

    /**
     * 生成文章
     *
     * @param prompt 写作提示词
     * @param style  写作风格
     * @return 统一响应（data.result 为生成内容）
     */
    ApiResponse<Map<String, Object>> generate(String prompt, String style);

    /**
     * 润色文本
     *
     * @param content 待润色文本
     * @return 统一响应（data.result 为润色后内容）
     */
    ApiResponse<Map<String, Object>> polish(String content);

    /**
     * 生成摘要
     *
     * @param content 原文
     * @return 统一响应（data.result 为摘要）
     */
    ApiResponse<Map<String, Object>> summarize(String content);

    /**
     * 扩写文本
     *
     * @param content 原文
     * @return 统一响应（data.result 为扩写后内容）
     */
    ApiResponse<Map<String, Object>> expand(String content);

    /**
     * 提取标签
     *
     * @param content 原文
     * @return 统一响应（data.result 为标签列表）
     */
    ApiResponse<Map<String, Object>> generateTags(String content);

    /**
     * 对话（非流式）
     *
     * @param message     用户消息
     * @param historyJson 历史对话 JSON
     * @return 统一响应（data.result 为回复内容）
     */
    ApiResponse<Map<String, Object>> chat(String message, String historyJson);

    /**
     * 流式对话：将大模型增量内容以 data: 格式写入 out，结束时写 data: [DONE]
     *
     * @param message     用户消息
     * @param historyJson 历史对话 JSON
     * @param out         输出流（SSE 响应）
     * @throws Exception 流式调用异常
     */
    void streamChat(String message, String historyJson, OutputStream out) throws Exception;

    /**
     * 代码生成
     *
     * @param prompt   代码需求描述
     * @param language 目标语言
     * @return 统一响应（data.result 为代码）
     */
    ApiResponse<Map<String, Object>> code(String prompt, String language);

    /**
     * 文本翻译
     *
     * @param text       原文
     * @param targetLang 目标语言
     * @return 统一响应（data.result 为译文）
     */
    ApiResponse<Map<String, Object>> translate(String text, String targetLang);

    /**
     * 标题优化
     *
     * @param content 文章内容
     * @return 统一响应（data.result 为优化后的标题）
     */
    ApiResponse<Map<String, Object>> headline(String content);

    /**
     * SEO 优化
     *
     * @param content 文章内容
     * @return 统一响应（data.result 为 SEO 建议）
     */
    ApiResponse<Map<String, Object>> seo(String content);

    /**
     * 生成大纲
     *
     * @param prompt 主题
     * @return 统一响应（data.result 为大纲）
     */
    ApiResponse<Map<String, Object>> outline(String prompt);

    /**
     * 生成社交媒体文案
     *
     * @param content 原文
     * @return 统一响应（data.result 为社交文案）
     */
    ApiResponse<Map<String, Object>> socialPost(String content);

    /**
     * 语法纠错
     *
     * @param content 原文
     * @return 统一响应（data.result 为纠错后内容）
     */
    ApiResponse<Map<String, Object>> grammarFix(String content);
}

package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    /** 模型名可配置：兼容 OpenAI 协议的服务商（DeepSeek/Moonshot/通义/智谱等）切换时只需改 api-url + model */
    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private static final int MAX_TOKENS = 8192;
    private static final int CHUNK_CHARS = 18000;

    @Override
    public ApiResponse<Map<String, Object>> generate(String prompt, String style) {
        String systemPrompt = "你是专业的内容创作助手。根据用户提供的关键词和要求，生成高质量的文章内容。";
        String userPrompt = String.format("请根据以下关键词生成一篇文章：\n关键词：%s\n风格：%s", prompt, style);
        return callAi(systemPrompt, userPrompt);
    }

    @Override
    public ApiResponse<Map<String, Object>> polish(String content) {
        String systemPrompt = "你是专业的文字编辑。请润色以下文章，提升文笔和可读性，保持原意不变。";
        return transformChunked(systemPrompt, "请润色以下内容：\n", content);
    }

    @Override
    public ApiResponse<Map<String, Object>> summarize(String content) {
        String systemPrompt = "你是专业的摘要助手。请用简洁的语言总结以下文章的核心要点。";
        if (content.length() > CHUNK_CHARS) {
            StringBuilder combined = new StringBuilder();
            int idx = 1;
            for (String chunk : splitChunks(content)) {
                ApiResponse<Map<String, Object>> r = callAi(systemPrompt, "请总结以下内容的第" + idx + "部分：\n" + chunk);
                if (!r.isSuccess()) return r;
                Object c = r.getData().get("content");
                if (c != null) combined.append(c).append("\n");
                idx++;
            }
            return callAi(systemPrompt, "以下是各部分的摘要，请整合成一篇完整摘要：\n" + combined);
        }
        return callAi(systemPrompt, "请总结以下内容：\n" + content);
    }

    @Override
    public ApiResponse<Map<String, Object>> expand(String content) {
        String systemPrompt = "你是专业的内容扩展助手。请在保持逻辑连贯的前提下，丰富和扩展以下内容。";
        return callAi(systemPrompt, "请扩展以下内容：\n" + content);
    }

    @Override
    public ApiResponse<Map<String, Object>> generateTags(String content) {
        String systemPrompt = "你是专业的标签生成助手。请为文章提取3-5个关键词标签，用逗号分隔，只返回标签。";
        if (content.length() > CHUNK_CHARS) {
            StringBuilder combined = new StringBuilder();
            int idx = 1;
            for (String chunk : splitChunks(content)) {
                ApiResponse<Map<String, Object>> r = callAi(systemPrompt, "请为以下内容生成标签（第" + idx + "部分）：\n" + chunk);
                if (!r.isSuccess()) return r;
                Object c = r.getData().get("content");
                if (c != null) combined.append(c).append(",");
                idx++;
            }
            return callAi(systemPrompt, "请从以下各部分的标签中选出最合适的3-8个：\n" + combined);
        }
        return callAi(systemPrompt, "请为以下内容生成标签：\n" + content);
    }

    @Override
    public ApiResponse<Map<String, Object>> chat(String message, String historyJson) {
        String systemPrompt = "你是智能博客助手，能够回答各种问题、提供创作建议、解释概念。回答简洁实用。";
        return callAiWithHistory(systemPrompt, message, historyJson);
    }

    private static final String CHAT_SYSTEM_PROMPT =
            "你是智能博客助手，能够回答各种问题、提供创作建议、解释概念。回答简洁实用。";

    @Override
    public void streamChat(String message, String historyJson, OutputStream out) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            out.write("data: [ERROR]DeepSeek API Key 未配置\n\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            return;
        }

        List<Map<String, String>> messages = buildMessages(CHAT_SYSTEM_PROMPT, message, historyJson);
        String requestBody = MAPPER.writeValueAsString(Map.of(
                "model", model,
                "messages", messages,
                "max_tokens", MAX_TOKENS,
                "temperature", 0.7,
                "stream", true
        ));

        HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(300000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) {
            out.write(("data: [ERROR]AI 服务异常 (HTTP " + conn.getResponseCode() + ")\n\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) continue;
                try {
                    JsonNode node = MAPPER.readTree(data);
                    JsonNode choices = node.get("choices");
                    if (choices == null || choices.isEmpty()) continue;
                    JsonNode delta = choices.get(0).get("delta");
                    if (delta == null) continue;
                    JsonNode content = delta.get("content");
                    if (content != null && !content.asText().isEmpty()) {
                        out.write(("data: " + content.asText() + "\n\n").getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    }
                } catch (Exception ignored) {
                    // 忽略无法解析的分片
                }
            }
        }
        out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    @Override
    public ApiResponse<Map<String, Object>> code(String prompt, String language) {
        String systemPrompt = "你是专业程序员，精通各种编程语言。请根据需求生成或解释代码，code模式或explain模式。";
        return callAi(systemPrompt, prompt);
    }

    @Override
    public ApiResponse<Map<String, Object>> translate(String text, String targetLang) {
        String systemPrompt = "你是专业翻译。请将提供的文本翻译，" + targetLang + "结果仅返回翻译后的文本。";
        return transformChunked(systemPrompt, "请翻译以下内容：\n", text);
    }

    @Override
    public ApiResponse<Map<String, Object>> headline(String content) {
        String systemPrompt = "你是标题优化专家。请为文章生成3-5个吸引人的标题建议，每个标题一行。";
        if (content.length() > CHUNK_CHARS) {
            ApiResponse<Map<String, Object>> sum = summarize(content);
            if (!sum.isSuccess()) return sum;
            Object c = sum.getData().get("content");
            return callAi(systemPrompt, "请为以下摘要生成优化标题：\n" + c);
        }
        return callAi(systemPrompt, "请为以下内容生成优化标题：\n" + content);
    }

    @Override
    public ApiResponse<Map<String, Object>> seo(String content) {
        String systemPrompt = "你是SEO优化专家。请分析文章并给出：关键词建议、meta描述、标题优化、内容结构建议。用中文简洁回答。";
        if (content.length() > CHUNK_CHARS) {
            ApiResponse<Map<String, Object>> sum = summarize(content);
            if (!sum.isSuccess()) return sum;
            Object c = sum.getData().get("content");
            return callAi(systemPrompt, "请分析以下文章摘要的SEO优化：\n" + c);
        }
        return callAi(systemPrompt, "请分析以下内容的SEO优化：\n" + content);
    }

    @Override
    public ApiResponse<Map<String, Object>> outline(String prompt) {
        String systemPrompt = "你是内容策划专家。请根据主题生成详细文章大纲，包含：摘要、3-5个章节标题、每个章节的关键点。";
        return callAi(systemPrompt, "请为主题生成文章大纲：" + prompt);
    }

    @Override
    public ApiResponse<Map<String, Object>> socialPost(String content) {
        String systemPrompt = "你是社交媒体运营专家。请从文章提取核心观点，生成适合微博/朋友圈/Twitter发布的文案(100字内)和一条详细推文。";
        if (content.length() > CHUNK_CHARS) {
            ApiResponse<Map<String, Object>> sum = summarize(content);
            if (!sum.isSuccess()) return sum;
            Object c = sum.getData().get("content");
            return callAi(systemPrompt, "请为以下摘要生成社交文案：\n" + c);
        }
        return callAi(systemPrompt, "请为以下内容生成社交文案：\n" + content);
    }

    @Override
    public ApiResponse<Map<String, Object>> grammarFix(String content) {
        String systemPrompt = "你是中文校对专家。请检查文本的语法错误、错别字、标点问题，并给出修正后的版本。先指出问题，再给出修正结果。";
        return transformChunked(systemPrompt, "请校对以下内容：\n", content);
    }

    private List<String> splitChunks(String content) {
        List<String> chunks = new ArrayList<>();
        int len = content.length();
        for (int start = 0; start < len; start += CHUNK_CHARS) {
            int end = Math.min(start + CHUNK_CHARS, len);
            if (end < len) {
                int nl = content.lastIndexOf('\n', end);
                if (nl > start + CHUNK_CHARS / 2) end = nl;
            }
            chunks.add(content.substring(start, end));
        }
        return chunks;
    }

    private ApiResponse<Map<String, Object>> transformChunked(String systemPrompt, String prefix, String content) {
        if (content.length() <= CHUNK_CHARS) {
            return callAi(systemPrompt, prefix + content);
        }
        StringBuilder result = new StringBuilder();
        int idx = 1;
        for (String chunk : splitChunks(content)) {
            ApiResponse<Map<String, Object>> r = callAi(systemPrompt,
                    prefix + "【第" + idx + "部分】\n" + chunk + "\n\n只输出本部分的处理结果，不要重复原文其余部分。");
            if (!r.isSuccess()) return r;
            Object c = r.getData().get("content");
            if (c != null) result.append(c).append("\n\n");
            idx++;
        }
        return ApiResponse.success("AI 处理完成", Map.of("content", result.toString()));
    }

    private ApiResponse<Map<String, Object>> callAi(String systemPrompt, String userPrompt) {
        return callAiRaw(systemPrompt, userPrompt, null);
    }

    private ApiResponse<Map<String, Object>> callAiWithHistory(String systemPrompt, String userPrompt, String historyJson) {
        return callAiRaw(systemPrompt, userPrompt, historyJson);
    }

    private ApiResponse<Map<String, Object>> callAiRaw(String systemPrompt, String userPrompt, String historyJson) {
        if (apiKey == null || apiKey.isBlank()) {
            return ApiResponse.fail("DeepSeek API Key 未配置，请设置环境变量 DEEPSEEK_API_KEY");
        }

        try {
            List<Map<String, String>> messages = buildMessages(systemPrompt, userPrompt, historyJson);

            String requestBody = MAPPER.writeValueAsString(Map.of(
                "model", model,
                "messages", messages,
                "max_tokens", MAX_TOKENS,
                "temperature", 0.7
            ));

            HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(300000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                StringBuilder err = new StringBuilder();
                try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8)) {
                    while (scanner.hasNextLine()) { err.append(scanner.nextLine()); }
                }
                log.error("DeepSeek API 错误: HTTP {} {}", conn.getResponseCode(), err);
                return ApiResponse.fail("AI 服务暂时不可用，请稍后重试");
            }

            StringBuilder response = new StringBuilder();
            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                while (scanner.hasNextLine()) { response.append(scanner.nextLine()); }
            }

            JsonNode root = MAPPER.readTree(response.toString());
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                return ApiResponse.fail("AI 未返回有效内容");
            }
            JsonNode message = choices.get(0).get("message");
            String content = message != null ? message.get("content").asText() : null;
            if (content == null || content.isBlank()) {
                return ApiResponse.fail("AI 未返回有效内容");
            }
            return ApiResponse.success("AI 处理完成", Map.of("content", content));
        } catch (Exception e) {
            log.error("调用 DeepSeek API 异常", e);
            return ApiResponse.fail("AI 服务异常，请稍后重试");
        }
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String userPrompt, String historyJson) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (historyJson != null && !historyJson.isBlank()) {
            try {
                JsonNode arr = MAPPER.readTree(historyJson);
                for (JsonNode m : arr) {
                    messages.add(Map.of("role", m.get("role").asText(), "content", m.get("content").asText()));
                }
            } catch (Exception ignored) {}
        }
        messages.add(Map.of("role", "user", "content", userPrompt));
        return messages;
    }

}

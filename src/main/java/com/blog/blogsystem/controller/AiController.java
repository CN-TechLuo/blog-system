package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.service.AiService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        String style = body.getOrDefault("style", "通用");
        if (prompt.isBlank()) {
            return ApiResponse.fail("请输入关键词或主题");
        }
        return aiService.generate(prompt, style);
    }

    @PostMapping("/polish")
    public ApiResponse<Map<String, Object>> polish(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) {
            return ApiResponse.fail("内容不能为空");
        }
        return aiService.polish(content);
    }

    @PostMapping("/summarize")
    public ApiResponse<Map<String, Object>> summarize(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) {
            return ApiResponse.fail("内容不能为空");
        }
        return aiService.summarize(content);
    }

    @PostMapping("/expand")
    public ApiResponse<Map<String, Object>> expand(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) {
            return ApiResponse.fail("内容不能为空");
        }
        return aiService.expand(content);
    }

    @PostMapping("/tags")
    public ApiResponse<Map<String, Object>> tags(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return aiService.generateTags(content);
    }

    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        String history = body.getOrDefault("history", "");
        if (message.isBlank()) return ApiResponse.fail("请输入内容");
        return aiService.chat(message, history);
    }

    @PostMapping("/chat/stream")
    public ResponseEntity<StreamingResponseBody> chatStream(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        String history = body.getOrDefault("history", "");
        if (message.isBlank()) {
            return ResponseEntity.badRequest().body(out -> {
                out.write("data: [ERROR]请输入内容\n\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.flush();
            });
        }
        StreamingResponseBody stream = out -> {
            try {
                aiService.streamChat(message, history, out);
            } catch (Exception e) {
                out.write("data: [ERROR]AI 服务异常\n\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.flush();
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(stream);
    }

    @PostMapping("/code")
    public ApiResponse<Map<String, Object>> code(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        String language = body.getOrDefault("language", "Java");
        if (prompt.isBlank()) return ApiResponse.fail("请输入需求");
        return aiService.code(prompt, language);
    }

    @PostMapping("/translate")
    public ApiResponse<Map<String, Object>> translate(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        String targetLang = body.getOrDefault("targetLang", "翻译为英文");
        if (text.isBlank()) return ApiResponse.fail("请输入文本");
        return aiService.translate(text, targetLang);
    }

    @PostMapping("/headline")
    public ApiResponse<Map<String, Object>> headline(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return aiService.headline(content);
    }

    @PostMapping("/seo")
    public ApiResponse<Map<String, Object>> seo(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return aiService.seo(content);
    }

    @PostMapping("/outline")
    public ApiResponse<Map<String, Object>> outline(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        if (prompt.isBlank()) return ApiResponse.fail("请输入主题");
        return aiService.outline(prompt);
    }

    @PostMapping("/social")
    public ApiResponse<Map<String, Object>> social(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return aiService.socialPost(content);
    }

    @PostMapping("/grammar")
    public ApiResponse<Map<String, Object>> grammar(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return aiService.grammarFix(content);
    }
}

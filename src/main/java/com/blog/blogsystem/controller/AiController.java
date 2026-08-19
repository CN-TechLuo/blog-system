package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.service.AiService;
import com.blog.blogsystem.service.AiUsageService;
import com.blog.blogsystem.util.SensitiveWordFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 助手", description = "DeepSeek 驱动的写作/聊天能力（登录用户，含每日配额）")
public class AiController {

    private final AiService aiService;
    private final AiUsageService aiUsageService;

    public AiController(AiService aiService, AiUsageService aiUsageService) {
        this.aiService = aiService;
        this.aiUsageService = aiUsageService;
    }

    /**
     * 统一守卫：内容审核 + 每日配额检查 + 用量记录
     */
    private ApiResponse<Map<String, Object>> guarded(HttpServletRequest request, String type, String input,
                                                     Supplier<ApiResponse<Map<String, Object>>> fn) {
        Integer userId = (Integer) request.getAttribute("userId");
        String quotaErr = aiUsageService.checkQuota(userId);
        if (quotaErr != null) return ApiResponse.fail(quotaErr);
        if (SensitiveWordFilter.containsSensitive(input)) {
            return ApiResponse.fail("内容包含违规词汇，无法处理");
        }
        ApiResponse<Map<String, Object>> result = fn.get();
        int outputChars = 0;
        if (result.isSuccess() && result.getData() != null && result.getData().get("content") != null) {
            outputChars = String.valueOf(result.getData().get("content")).length();
        }
        aiUsageService.record(userId, type, input == null ? 0 : input.length(), outputChars);
        return result;
    }

    @PostMapping("/generate")
    @Operation(summary = "文章生成")
    public ApiResponse<Map<String, Object>> generate(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String prompt = body.getOrDefault("prompt", "");
        String style = body.getOrDefault("style", "通用");
        if (prompt.isBlank()) return ApiResponse.fail("请输入关键词或主题");
        return guarded(request, "generate", prompt, () -> aiService.generate(prompt, style));
    }

    @PostMapping("/polish")
    @Operation(summary = "润色")
    public ApiResponse<Map<String, Object>> polish(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return guarded(request, "polish", content, () -> aiService.polish(content));
    }

    @PostMapping("/summarize")
    @Operation(summary = "摘要")
    public ApiResponse<Map<String, Object>> summarize(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return guarded(request, "summarize", content, () -> aiService.summarize(content));
    }

    @PostMapping("/expand")
    @Operation(summary = "扩写")
    public ApiResponse<Map<String, Object>> expand(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return guarded(request, "expand", content, () -> aiService.expand(content));
    }

    @PostMapping("/tags")
    @Operation(summary = "标签生成")
    public ApiResponse<Map<String, Object>> tags(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return guarded(request, "tags", content, () -> aiService.generateTags(content));
    }

    @PostMapping("/chat")
    @Operation(summary = "对话")
    public ApiResponse<Map<String, Object>> chat(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String message = body.getOrDefault("message", "");
        String history = body.getOrDefault("history", "");
        if (message.isBlank()) return ApiResponse.fail("请输入内容");
        return guarded(request, "chat", message, () -> aiService.chat(message, history));
    }

    @PostMapping("/chat/stream")
    @Operation(summary = "流式对话")
    public ResponseEntity<StreamingResponseBody> chatStream(@RequestBody Map<String, String> body,
                                                            HttpServletRequest request) {
        String message = body.getOrDefault("message", "");
        String history = body.getOrDefault("history", "");
        Integer userId = (Integer) request.getAttribute("userId");
        if (message.isBlank()) {
            return errorStream("请输入内容");
        }
        String quotaErr = aiUsageService.checkQuota(userId);
        if (quotaErr != null) {
            return errorStream(quotaErr);
        }
        if (SensitiveWordFilter.containsSensitive(message)) {
            return errorStream("内容包含违规词汇，无法处理");
        }
        StreamingResponseBody stream = out -> {
            try {
                aiService.streamChat(message, history, out);
                aiUsageService.record(userId, "chat_stream", message.length(), 0);
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

    private ResponseEntity<StreamingResponseBody> errorStream(String msg) {
        return ResponseEntity.badRequest().body(out -> {
            out.write(("data: [ERROR]" + msg + "\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
        });
    }

    @PostMapping("/code")
    @Operation(summary = "代码生成")
    public ApiResponse<Map<String, Object>> code(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String prompt = body.getOrDefault("prompt", "");
        String language = body.getOrDefault("language", "Java");
        if (prompt.isBlank()) return ApiResponse.fail("请输入需求");
        return guarded(request, "code", prompt, () -> aiService.code(prompt, language));
    }

    @PostMapping("/translate")
    @Operation(summary = "翻译")
    public ApiResponse<Map<String, Object>> translate(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String text = body.getOrDefault("text", "");
        String targetLang = body.getOrDefault("targetLang", "翻译为英文");
        if (text.isBlank()) return ApiResponse.fail("请输入文本");
        return guarded(request, "translate", text, () -> aiService.translate(text, targetLang));
    }

    @PostMapping("/headline")
    @Operation(summary = "标题优化")
    public ApiResponse<Map<String, Object>> headline(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return guarded(request, "headline", content, () -> aiService.headline(content));
    }

    @PostMapping("/seo")
    @Operation(summary = "SEO 优化")
    public ApiResponse<Map<String, Object>> seo(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return guarded(request, "seo", content, () -> aiService.seo(content));
    }

    @PostMapping("/outline")
    @Operation(summary = "大纲生成")
    public ApiResponse<Map<String, Object>> outline(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String prompt = body.getOrDefault("prompt", "");
        if (prompt.isBlank()) return ApiResponse.fail("请输入主题");
        return guarded(request, "outline", prompt, () -> aiService.outline(prompt));
    }

    @PostMapping("/social")
    @Operation(summary = "社交文案")
    public ApiResponse<Map<String, Object>> social(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return guarded(request, "social", content, () -> aiService.socialPost(content));
    }

    @PostMapping("/grammar")
    @Operation(summary = "语法校对")
    public ApiResponse<Map<String, Object>> grammar(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return ApiResponse.fail("内容不能为空");
        return guarded(request, "grammar", content, () -> aiService.grammarFix(content));
    }
}

package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ReportRequest;
import com.blog.blogsystem.entity.Report;
import com.blog.blogsystem.mapper.ArticleMapper;
import com.blog.blogsystem.mapper.CommentMapper;
import com.blog.blogsystem.mapper.ReportMapper;
import com.blog.blogsystem.util.AuditLogger;
import com.blog.blogsystem.util.RateLimiterUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 举报上报（需登录）：文章/评论的投诉举报入口，
 * 与管理端处置接口构成投诉举报闭环。
 */
@RestController
@RequestMapping("/api/report")
@Tag(name = "举报管理", description = "内容举报上报")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private static final Set<String> ALLOWED_REASONS = Set.of(
            "违法违规", "色情低俗", "垃圾广告", "人身攻击", "侵权", "其他");
    private static final int MAX_REPORT_PER_MINUTE = 10;

    private final ReportMapper reportMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;

    public ReportController(ReportMapper reportMapper, ArticleMapper articleMapper,
                            CommentMapper commentMapper) {
        this.reportMapper = reportMapper;
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
    }

    @PostMapping
    @Operation(summary = "提交举报")
    public ResponseEntity<ApiResponse<Void>> create(@RequestAttribute("userId") Integer userId,
                                                     @Valid @RequestBody ReportRequest request) {
        String targetType = request.getTargetType().trim();
        if (!"article".equals(targetType) && !"comment".equals(targetType)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("举报对象类型不合法"));
        }
        if (!ALLOWED_REASONS.contains(request.getReason().trim())) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("举报原因不合法"));
        }
        if (RateLimiterUtil.isBlocked("report:u:" + userId, MAX_REPORT_PER_MINUTE)) {
            return ResponseEntity.status(429).body(ApiResponse.fail("举报过于频繁，请稍后重试"));
        }

        boolean exists = "article".equals(targetType)
                ? articleMapper.findById(request.getTargetId()) != null
                : commentMapper.findById(request.getTargetId()) != null;
        if (!exists) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("举报对象不存在或已被删除"));
        }
        if (reportMapper.countPendingByReporterAndTarget(userId, targetType, request.getTargetId()) > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("您已举报过该内容，请勿重复提交"));
        }

        Report report = new Report();
        report.setReporterId(userId);
        report.setTargetType(targetType);
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason().trim());
        report.setDetail(request.getDetail() == null || request.getDetail().isBlank()
                ? null : request.getDetail().trim());
        int rows = reportMapper.insert(report);

        if (rows > 0) {
            AuditLogger.log("REPORT_SUBMIT", "userId=" + userId + ", targetType=" + targetType
                    + ", targetId=" + request.getTargetId() + ", reason=" + report.getReason());
            log.info("举报提交成功: userId={}, target={}:{}", userId, targetType, request.getTargetId());
            return ResponseEntity.ok(ApiResponse.success("举报已提交，平台将尽快核实处理"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.fail("举报提交失败，请稍后重试"));
    }

}

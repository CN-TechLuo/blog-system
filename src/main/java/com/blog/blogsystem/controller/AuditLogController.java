package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.util.RoleConst;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 安全审计日志查询（管理员）
 * 读取本地 audit.log 尾部 N 行，倒序返回。
 */
@RestController
@RequestMapping("/api/admin")
public class AuditLogController {

    private static final Logger log = LoggerFactory.getLogger(AuditLogController.class);

    private final UserMapper userMapper;

    @Value("${app.audit-log-path:logs/audit.log}")
    private String auditLogPath;

    public AuditLogController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "查询最近安全审计日志")
    public ResponseEntity<ApiResponse<List<String>>> auditLogs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "100") int limit) {
        Integer userId = (Integer) request.getAttribute("userId");
        User user = userId != null ? userMapper.findById(userId) : null;
        if (user == null || !RoleConst.ADMIN.equals(user.getRole())) {
            return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        }
        return ResponseEntity.ok(ApiResponse.success("查询成功", readTail(limit)));
    }

    private List<String> readTail(int limit) {
        int max = Math.min(Math.max(limit, 1), 500);
        List<String> lines = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(auditLogPath, "r")) {
            long length = raf.length();
            if (length == 0) return lines;
            long pointer = length - 1;
            StringBuilder current = new StringBuilder();
            while (pointer >= 0 && lines.size() < max) {
                raf.seek(pointer);
                int b = raf.read();
                if (b == '\n') {
                    if (current.length() > 0) {
                        lines.add(current.reverse().toString());
                        current.setLength(0);
                    }
                } else {
                    current.append((char) b);
                }
                pointer--;
            }
            if (current.length() > 0 && lines.size() < max) {
                lines.add(current.reverse().toString());
            }
            Collections.reverse(lines);
        } catch (IOException e) {
            log.warn("读取审计日志失败: {}", auditLogPath, e);
        }
        return lines;
    }
}

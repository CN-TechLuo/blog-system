package com.blog.blogsystme.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 安全审计日志工具（L14 审计日志）
 * 输出到独立的 AUDIT appender（logs/audit.log），与业务日志分离，
 * 用于记录登录成败、账号锁定、密码变更、管理员操作等安全事件。
 * 注意：禁止在审计日志中写入密码、令牌、验证码等敏感信息。
 */
public final class AuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private AuditLogger() {}

    public static void log(String event, String detail) {
        AUDIT.info("[{}] {}", event, detail);
    }

}

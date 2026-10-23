package com.blog.blogsystem.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 安全审计日志工具（L14 审计日志） 输出到独立的 AUDIT appender（logs/audit.log），与业务日志分离， 用于记录登录成败、密码变更、管理员操作等安全事件。 注意：禁止在审计日志中写入密码、令牌、验证码等敏感信息；
 * 邮箱/手机号在落盘和读取时统一脱敏，降低 PII 二次泄露面。
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public final class AuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final Pattern EMAIL = Pattern.compile(
            "([A-Za-z0-9._%+-]{1,64})@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern PHONE = Pattern.compile("\\b(1[3-9]\\d)\\d{4}(\\d{4})\\b");

    private AuditLogger() {}

    public static void log(String event, String detail) {
        AUDIT.info("[{}] {}", event, sanitize(detail));
    }

    /** 对邮箱/手机号做脱敏；审计日志写入与查询共用，兼容历史日志 */
    public static String sanitize(String detail) {
        if (detail == null || detail.isEmpty()) {
            return detail;
        }
        Matcher matcher = EMAIL.matcher(detail);
        StringBuilder masked = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(masked,
                    Matcher.quoteReplacement(MaskUtil.maskEmail(matcher.group())));
        }
        matcher.appendTail(masked);
        return PHONE.matcher(masked.toString()).replaceAll("$1****$2");
    }

}

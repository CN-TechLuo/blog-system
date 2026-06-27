package com.blog.blogsystme.util;

/**
 * XSS 过滤工具类
 * 对用户输入的 HTML 特殊字符进行转义，防止存储型 XSS 攻击
 */
public class XssUtil {

    // HTML 实体前缀
    private static final char AMP = '&';

    /**
     * 转义 HTML 特殊字符，防止 XSS 攻击
     * 通过字符拼接构建 HTML 实体，避免源码中出现 HTML 实体字符串
     * 被 IDE 误解码为字面字符而导致 Java 文本块语法歧义
     */
    public static String escape(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> sb.append(AMP).append("amp;");
                case '<' -> sb.append(AMP).append("lt;");
                case '>' -> sb.append(AMP).append("gt;");
                case '"' -> sb.append(AMP).append("quot;");
                case '\'' -> sb.append(AMP).append("#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

}
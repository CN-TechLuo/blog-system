package com.blog.blogsystme.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {

    /**
     * 密码复杂度正则：必须包含字母、数字和特殊字符，长度 8-50 位。
     * 供 {@code @Pattern} 注解引用，确保 RegisterRequest 和 User 实体使用统一的密码策略。
     */
    public static final String PASSWORD_REGEX =
        "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?`~]).+$";

    // 创建一个 BCryptPasswordEncoder 对象（单例，避免重复创建）
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 加密原始密码
     * @param rawPassword 用户输入的明文密码
     * @return 加密后的密文
     */
    public static String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 验证密码是否匹配
     * @param rawPassword 用户输入的明文密码
     * @param encodedPassword 数据库里存储的密文
     * @return true=密码正确，false=密码错误
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}

package com.blog.blogsystme.Util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {

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

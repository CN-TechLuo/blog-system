package com.blog.blogsystme.util;

public final class MaskUtil {

    private MaskUtil() {}

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@", 2);
        String name = parts[0];
        if (name.isEmpty()) return "***@" + parts[1];
        if (name.length() <= 2) return name.substring(0, 1) + "***@" + parts[1];
        return name.substring(0, 2) + "***@" + parts[1];
    }

    public static String maskUsername(String username) {
        if (username == null || username.isEmpty()) return username;
        if (username.length() <= 2) return username.substring(0, 1) + "*";
        if (username.length() <= 4) return username.substring(0, 1) + "***" + username.substring(username.length() - 1);
        return username.substring(0, 2) + "***" + username.substring(username.length() - 2);
    }
}

package com.blog.blogsystem.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析：只有请求确实来自可信代理时，才采信转发头。
 *
 * 为什么不能直接用 X-Forwarded-For 的第一个值：
 * nginx 的 {@code $proxy_add_x_forwarded_for} 会把客户端自带的 XFF 原样保留在最左侧，
 * 于是 {@code X-Forwarded-For: 1.2.3.4} 就能让每个请求换个"新 IP"，按 IP 的限流全部失效，
 * 审计日志里的 IP 也不再可信。
 *
 * 解析规则：
 * 1. remoteAddr 不是内网/回环 → 视为直连客户端，完全忽略 XFF / X-Real-IP；
 * 2. remoteAddr 是内网/回环（说明前面有代理）→ 从 XFF 右往左取第一个非内网地址，
 *    因为右侧的跳数才是代理追加的，左侧可能是调用方伪造的。
 */
public final class ClientIpUtil {

    private ClientIpUtil() {}

    public static String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!isTrustedProxy(remote)) {
            return remote;
        }

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] hops = xff.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = hops[i].trim();
                if (hop.isEmpty()) {
                    continue;
                }
                if (!isTrustedProxy(hop)) {
                    return hop;
                }
            }
            String first = hops[0].trim();
            return first.isEmpty() ? remote : first;
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return remote;
    }

    /** 内网/回环地址视为"可信代理跳数"；默认即 RFC1918 + 127/8 + ::1 + link-local */
    public static boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String v = ip.trim();
        if ("::1".equals(v) || "0:0:0:0:0:0:0:1".equals(v)) {
            return true;
        }
        if (v.indexOf(':') >= 0) {
            // 其他 IPv6：既无法段判断也不应被当作客户端 IP 使用，按不可信处理
            return false;
        }
        String[] parts = v.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        int a;
        int b;
        try {
            a = Integer.parseInt(parts[0]);
            b = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (a == 127 || a == 10) {
            return true;
        }
        if (a == 192 && b == 168) {
            return true;
        }
        if (a == 169 && b == 254) {
            return true;
        }
        return a == 172 && b >= 16 && b <= 31;
    }
}

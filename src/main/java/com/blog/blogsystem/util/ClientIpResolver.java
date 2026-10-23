package com.blog.blogsystem.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端真实 IP 解析：仅当直连来源命中可信代理 CIDR 时才采信转发头，
 * 并从 X-Forwarded-For 右侧向左跳过可信代理，取第一个不可信地址。
 *
 * @author CN-TechLuo
 * @date 2026/10/23
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);
    private static final int MAX_HEADER_LENGTH = 512;

    private record Cidr(byte[] network, int prefix) {}

    private final List<Cidr> trustedProxies;

    public ClientIpResolver(@Value("${app.trusted-proxies:}") String configured) {
        this.trustedProxies = parseCidrs(configured);
        if (trustedProxies.isEmpty()) {
            log.info("未配置可信反向代理，X-Forwarded-For/X-Real-IP 将被忽略");
        } else {
            log.info("可信反向代理 CIDR 数量: {}", trustedProxies.size());
        }
    }

    /**
     * 解析客户端 IP。
     *
     * @param request 当前请求
     * @return 真实客户端 IP；无法解析时返回 "unknown"
     */
    public String resolve(HttpServletRequest request) {
        String remote = normalize(request.getRemoteAddr());
        if (remote == null) {
            return "unknown";
        }
        if (!isTrusted(remote)) {
            return remote;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && forwarded.length() <= MAX_HEADER_LENGTH) {
            String[] hops = forwarded.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = normalize(hops[i].trim());
                if (hop != null && !isTrusted(hop)) {
                    return hop;
                }
            }
        }

        String realIp = normalize(request.getHeader("X-Real-IP"));
        return realIp != null ? realIp : remote;
    }

    private String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        int zone = value.indexOf('%');
        if (zone > 0) {
            value = value.substring(0, zone);
        }
        if (value.regionMatches(true, 0, "::ffff:", 0, 7) && value.indexOf('.') > 0) {
            value = value.substring(7);
        }
        if (!value.matches("[0-9a-fA-F:.]+")) {
            return null;
        }
        if (!value.contains(".") && !value.contains(":")) {
            return null;
        }
        try {
            return InetAddress.getByName(value).getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isTrusted(String ip) {
        if (trustedProxies.isEmpty()) {
            return false;
        }
        try {
            byte[] address = InetAddress.getByName(ip).getAddress();
            for (Cidr cidr : trustedProxies) {
                if (matches(address, cidr)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private boolean matches(byte[] address, Cidr cidr) {
        if (address.length != cidr.network().length) {
            return false;
        }
        int fullBytes = cidr.prefix() / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (address[i] != cidr.network()[i]) {
                return false;
            }
        }
        int remainingBits = cidr.prefix() % 8;
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return (address[fullBytes] & mask) == (cidr.network()[fullBytes] & mask);
    }

    private List<Cidr> parseCidrs(String configured) {
        List<Cidr> result = new ArrayList<>();
        if (configured == null || configured.isBlank()) {
            return List.copyOf(result);
        }
        for (String part : configured.split(",")) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                String[] pieces = value.split("/", 2);
                String ip = pieces[0].trim();
                if (!ip.matches("[0-9a-fA-F:.]+") || (!ip.contains(".") && !ip.contains(":"))) {
                    throw new IllegalArgumentException("非法 IP");
                }
                byte[] bytes = InetAddress.getByName(ip).getAddress();
                int prefix = pieces.length == 2
                        ? Integer.parseInt(pieces[1].trim())
                        : bytes.length * 8;
                if (prefix < 0 || prefix > bytes.length * 8) {
                    throw new IllegalArgumentException("非法前缀");
                }
                result.add(new Cidr(bytes, prefix));
            } catch (Exception e) {
                log.warn("忽略非法可信代理 CIDR: {}", value);
            }
        }
        return List.copyOf(result);
    }
}

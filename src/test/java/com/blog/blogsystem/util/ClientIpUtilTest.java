package com.blog.blogsystem.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientIpUtilTest {

    @Test
    void directClientShouldIgnoreSpoofedXff() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.7");
        req.addHeader("X-Forwarded-For", "1.2.3.4");
        req.addHeader("X-Real-IP", "5.6.7.8");
        assertEquals("203.0.113.7", ClientIpUtil.resolve(req), "直连时不得采信转发头");
    }

    @Test
    void proxyShouldTakeRightmostUntrustedHop() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        // 客户端伪造了最左值，nginx 用 $remote_addr 把真实来源追加到最右
        req.addHeader("X-Forwarded-For", "9.9.9.9, 198.51.100.23");
        assertEquals("198.51.100.23", ClientIpUtil.resolve(req), "应取代理追加的右侧跳数");
    }

    @Test
    void singleHopProxyShouldReturnClient() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("172.18.0.5");
        req.addHeader("X-Forwarded-For", "198.51.100.23");
        assertEquals("198.51.100.23", ClientIpUtil.resolve(req));
    }

    @Test
    void noForwardHeaderShouldFallBackToRemoteAddr() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        assertEquals("127.0.0.1", ClientIpUtil.resolve(req));
    }

    @Test
    void privateRangesAreTrustedProxies() {
        assertTrue(ClientIpUtil.isTrustedProxy("10.1.2.3"));
        assertTrue(ClientIpUtil.isTrustedProxy("192.168.1.1"));
        assertTrue(ClientIpUtil.isTrustedProxy("172.16.0.1"));
        assertTrue(ClientIpUtil.isTrustedProxy("172.31.255.255"));
        assertTrue(ClientIpUtil.isTrustedProxy("127.0.0.1"));
        assertTrue(ClientIpUtil.isTrustedProxy("::1"));
        assertFalse(ClientIpUtil.isTrustedProxy("172.32.0.1"));
        assertFalse(ClientIpUtil.isTrustedProxy("8.8.8.8"));
        assertFalse(ClientIpUtil.isTrustedProxy(null));
    }
}

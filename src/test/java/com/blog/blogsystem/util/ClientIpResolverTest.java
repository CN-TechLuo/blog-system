package com.blog.blogsystem.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void directClientIgnoresForwardedHeaders() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.5");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        request.addHeader("X-Real-IP", "5.6.7.8");

        assertEquals("203.0.113.5", resolver.resolve(request));
    }

    @Test
    void trustedProxyUsesRightmostUntrustedHop() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8,172.16.0.0/12");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.9, 172.18.0.4");

        assertEquals("203.0.113.9", resolver.resolve(request));
    }

    @Test
    void allTrustedHopsFallBackToRemote() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Forwarded-For", "10.2.3.4, 10.9.9.9");

        assertEquals("10.1.2.3", resolver.resolve(request));
    }

    @Test
    void malformedForwardedHeaderFallsBackToRemote() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Forwarded-For", "not-an-ip, <script>");

        assertEquals("10.1.2.3", resolver.resolve(request));
    }

    @Test
    void emptyTrustedProxyConfigIgnoresForwardedHeaders() {
        ClientIpResolver resolver = new ClientIpResolver("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertEquals("10.1.2.3", resolver.resolve(request));
    }
}

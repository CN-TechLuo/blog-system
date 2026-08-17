package com.blog.blogsystem.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XssUtilTest {

    @Test
    void escapeShouldEncodeHtmlSpecialChars() {
        assertEquals("&lt;script&gt;", XssUtil.escape("<script>"));
        assertEquals("&quot;&#39;", XssUtil.escape("\"'"));
    }

    @Test
    void escapeShouldHandleNullAndEmpty() {
        assertNull(XssUtil.escape(null));
        assertEquals("", XssUtil.escape(""));
    }

    @Test
    void sanitizeShouldStripScriptTags() {
        String dirty = "<p>正文</p><script>alert('xss')</script><img src='http://evil.com/x.png' onerror='alert(1)'>";
        String clean = XssUtil.sanitizeHtml(dirty);
        assertFalse(clean.contains("<script"));
        assertFalse(clean.contains("onerror"));
        assertTrue(clean.contains("<p>正文</p>"));
    }

    @Test
    void sanitizeShouldKeepSafeTags() {
        String html = "<h2>标题</h2><p><a href='https://example.com' target='_blank'>链接</a></p><pre><code>code</code></pre>";
        String clean = XssUtil.sanitizeHtml(html);
        assertTrue(clean.contains("<h2>标题</h2>"));
        assertTrue(clean.contains("href=\"https://example.com\""));
        assertTrue(clean.contains("<pre><code>code</code></pre>"));
    }

    @Test
    void sanitizeShouldBlockJavascriptProtocol() {
        String dirty = "<a href='javascript:alert(1)'>click</a>";
        String clean = XssUtil.sanitizeHtml(dirty);
        assertFalse(clean.contains("javascript:"));
    }

}

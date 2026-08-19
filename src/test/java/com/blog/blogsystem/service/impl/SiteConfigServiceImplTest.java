package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.entity.SiteConfig;
import com.blog.blogsystem.mapper.SiteConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteConfigServiceImplTest {

    private SiteConfigMapper mapper;
    private SiteConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SiteConfigMapper.class);
        service = new SiteConfigServiceImpl(mapper);
    }

    @Test
    void shouldReturnEmptyWhenNoConfig() {
        when(mapper.findConfig()).thenReturn(null);
        assertEquals("", service.getContactEmail());
    }

    @Test
    void shouldTrimEmail() {
        SiteConfig config = new SiteConfig();
        config.setContactEmail(" admin@example.com ");
        when(mapper.findConfig()).thenReturn(config);
        assertEquals("admin@example.com", service.getContactEmail());
    }

    @Test
    void shouldRejectInvalidEmail() {
        assertFalse(service.updateContactEmail("not-an-email"));
        verify(mapper, never()).updateContactEmail(anyString());
    }

    @Test
    void shouldAcceptValidEmailAndEmpty() {
        when(mapper.updateContactEmail(anyString())).thenReturn(1);
        assertTrue(service.updateContactEmail("admin@example.com"));
        assertTrue(service.updateContactEmail(""));
        assertTrue(service.updateContactEmail(null));
    }
}

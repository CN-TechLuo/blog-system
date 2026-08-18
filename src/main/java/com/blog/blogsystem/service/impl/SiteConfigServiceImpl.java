package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.entity.SiteConfig;
import com.blog.blogsystem.mapper.SiteConfigMapper;
import com.blog.blogsystem.service.SiteConfigService;
import org.springframework.stereotype.Service;

@Service
public class SiteConfigServiceImpl implements SiteConfigService {

    private static final String EMAIL_REGEX = "^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$";

    private final SiteConfigMapper siteConfigMapper;

    public SiteConfigServiceImpl(SiteConfigMapper siteConfigMapper) {
        this.siteConfigMapper = siteConfigMapper;
    }

    @Override
    public String getContactEmail() {
        SiteConfig config = siteConfigMapper.findConfig();
        String email = config != null ? config.getContactEmail() : null;
        return email == null ? "" : email.trim();
    }

    @Override
    public boolean updateContactEmail(String email) {
        if (email == null) email = "";
        email = email.trim();
        if (!email.isEmpty() && !email.matches(EMAIL_REGEX)) {
            return false;
        }
        return siteConfigMapper.updateContactEmail(email) > 0;
    }
}

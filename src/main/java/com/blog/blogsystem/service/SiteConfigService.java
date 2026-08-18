package com.blog.blogsystem.service;

public interface SiteConfigService {

    String getContactEmail();

    boolean updateContactEmail(String email);
}

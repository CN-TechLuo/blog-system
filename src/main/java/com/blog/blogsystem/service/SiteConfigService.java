package com.blog.blogsystem.service;

/**
 * 站点配置服务：管理员联系邮箱读写
 *
 * @author CN-TechLuo
 * @date 2026/08/19
 */
public interface SiteConfigService {

    /**
     * 查询管理员联系邮箱（公开接口，未配置返回空串）
     *
     * @return 联系邮箱
     */
    String getContactEmail();

    /**
     * 更新管理员联系邮箱（配置不存在时自动初始化）
     *
     * @param email 新联系邮箱
     * @return true=成功 false=失败
     */
    boolean updateContactEmail(String email);
}

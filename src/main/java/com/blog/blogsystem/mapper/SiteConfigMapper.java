package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.SiteConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 站点配置数据访问接口
 *
 * @author CN-TechLuo
 * @date 2026/08/19
 */
@Mapper
public interface SiteConfigMapper {

    /**
     * 查询站点配置（单行表，固定 id=1）
     *
     * @return 站点配置，未初始化返回 null
     */
    @Select("SELECT * FROM site_config WHERE id = 1")
    SiteConfig findConfig();

    /**
     * 更新管理员联系邮箱
     *
     * @param email 新的联系邮箱
     * @return 影响行数
     */
    @Update("UPDATE site_config SET contact_email = #{email} WHERE id = 1")
    int updateContactEmail(@Param("email") String email);
}

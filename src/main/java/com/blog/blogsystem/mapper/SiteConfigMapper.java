package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.SiteConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SiteConfigMapper {

    @Select("SELECT * FROM site_config WHERE id = 1")
    SiteConfig findConfig();

    @Update("UPDATE site_config SET contact_email = #{email} WHERE id = 1")
    int updateContactEmail(@Param("email") String email);
}

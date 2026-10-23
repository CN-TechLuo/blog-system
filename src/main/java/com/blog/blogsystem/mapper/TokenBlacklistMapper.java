package com.blog.blogsystem.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JWT 黑名单数据访问接口（登出/改密吊销）
 *
 * @author CN-TechLuo
 * @date 2026/08/19
 */
@Mapper
public interface TokenBlacklistMapper {

    /**
     * 将 token 的 jti 加入黑名单
     *
     * @param jti        token 唯一标识
     * @param expireTime 过期时间（到期后可清理）
     * @return 影响行数
     */
    @Insert("INSERT INTO token_blacklist (jti, expire_time) VALUES (#{jti}, #{expireTime})")
    int insert(@Param("jti") String jti, @Param("expireTime") LocalDateTime expireTime);

    /**
     * 查询 jti 是否在黑名单中
     *
     * @param jti token 唯一标识
     * @return 命中返回 jti，否则 null
     */
    @Select("SELECT jti FROM token_blacklist WHERE jti = #{jti}")
    String findByJti(@Param("jti") String jti);

    /**
     * 查询未过期的黑名单 jti 列表（应用启动时预热缓存）
     *
     * @return jti 列表
     */
    @Select("SELECT jti FROM token_blacklist WHERE expire_time > NOW()")
    List<String> findActive();

    /**
     * 清理已过期的黑名单记录（定时任务）
     *
     * @return 影响行数
     */
    @Delete("DELETE FROM token_blacklist WHERE expire_time <= NOW()")
    int deleteExpired();
}

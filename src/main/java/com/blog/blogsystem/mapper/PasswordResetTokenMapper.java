package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.PasswordResetToken;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 密码重置令牌数据访问接口
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Mapper
public interface PasswordResetTokenMapper {

    /**
     * 新增密码重置令牌（存储令牌哈希，不存明文）
     *
     * @param token 令牌实体
     * @return 影响行数
     */
    @Insert("INSERT INTO password_reset_token (user_id, token_hash, expires_at) VALUES (#{userId}, #{tokenHash}, #{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PasswordResetToken token);

    /**
     * 按令牌哈希查询未使用的令牌（取最新一条）
     *
     * @param tokenHash 令牌 SHA-256 哈希
     * @return 令牌实体，不存在返回 null
     */
    @Select("SELECT * FROM password_reset_token WHERE token_hash = #{tokenHash} AND used = 0 ORDER BY id DESC LIMIT 1")
    PasswordResetToken findByHash(@Param("tokenHash") String tokenHash);

    /**
     * 标记令牌已使用（防重放）
     *
     * @param id 令牌ID
     * @return 影响行数
     */
    @Update("UPDATE password_reset_token SET used = 1 WHERE id = #{id}")
    int markUsed(@Param("id") Integer id);

    /**
     * 作废用户所有未使用的令牌（新令牌申请后旧令牌失效）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE password_reset_token SET used = 1 WHERE user_id = #{userId} AND used = 0")
    int invalidateAllForUser(@Param("userId") Integer userId);

    /**
     * 删除用户的全部令牌（账号注销时级联清理）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM password_reset_token WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * 清理过期或已使用超过 1 天的令牌，防止表无限增长
     *
     * @return 删除行数
     */
    @Delete("DELETE FROM password_reset_token WHERE expires_at < NOW() " +
            "OR (used = 1 AND create_time < NOW() - INTERVAL 1 DAY)")
    int deleteExpired();

}

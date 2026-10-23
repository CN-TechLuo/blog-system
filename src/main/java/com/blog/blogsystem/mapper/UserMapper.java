package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户数据访问接口（含锁定字段与令牌版本更新）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Mapper
public interface UserMapper {

    /**
     * 新增用户
     *
     * @param user 用户实体
     * @return 影响行数
     */
    @Insert("INSERT INTO user (username, nickname, password, email, phone) VALUES (#{username}, #{nickname}, #{password}, #{email}, #{phone})")
    int insert(User user);

    /**
     * 按用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 按手机号查询用户
     *
     * @param phone 手机号
     * @return 用户实体，不存在返回 null
     */
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    /**
     * 按邮箱查询用户（V4 起 email 唯一，最多返回 1 条）
     *
     * @param email 邮箱
     * @return 用户列表
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    List<User> findByEmail(@Param("email") String email);

    /**
     * 按 ID 查询用户
     *
     * @param id 用户ID
     * @return 用户实体，不存在返回 null
     */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    /**
     * 批量按 ID 查询用户（列表页批量填充作者信息）
     *
     * @param ids 用户ID集合
     * @return 用户列表
     */
    @Select("<script>SELECT * FROM user WHERE id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<User> findByIds(@Param("ids") List<Integer> ids);

    /**
     * 更新用户密码
     *
     * @param id       用户ID
     * @param password 新密码哈希
     * @return 影响行数
     */
    @Update("UPDATE user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    /**
     * 查询用户令牌版本号
     *
     * @param id 用户ID
     * @return 令牌版本号
     */
    @Select("SELECT token_version FROM user WHERE id = #{id}")
    Integer findTokenVersion(@Param("id") Integer id);

    /**
     * 令牌版本号加一（改密/登出时使旧 refresh token 全部失效）
     *
     * @param id 用户ID
     * @return 影响行数
     */
    @Update("UPDATE user SET token_version = token_version + 1 WHERE id = #{id}")
    int incrementTokenVersion(@Param("id") Integer id);

    /**
     * 更新用户头像
     *
     * @param id        用户ID
     * @param avatarUrl 新头像URL
     * @return 影响行数
     */
    @Update("UPDATE user SET avatar_url = #{avatarUrl} WHERE id = #{id}")
    int updateAvatar(@Param("id") Integer id, @Param("avatarUrl") String avatarUrl);

    /**
     * 更新用户昵称
     *
     * @param id       用户ID
     * @param nickname 新昵称
     * @return 影响行数
     */
    @Update("UPDATE user SET nickname = #{nickname} WHERE id = #{id}")
    int updateNickname(@Param("id") Integer id, @Param("nickname") String nickname);

    /**
     * 更新用户手机号
     *
     * @param id    用户ID
     * @param phone 新手机号
     * @return 影响行数
     */
    @Update("UPDATE user SET phone = #{phone} WHERE id = #{id}")
    int updatePhone(@Param("id") Integer id, @Param("phone") String phone);

    /**
     * 查询全部用户（管理端）
     *
     * @return 用户列表
     */
    @Select("SELECT * FROM user ORDER BY id")
    List<User> findAll();

    /**
     * 分页查询用户（管理端）
     *
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 用户列表
     */
    @Select("SELECT * FROM user ORDER BY id LIMIT #{start}, #{pageSize}")
    List<User> findByPage(@Param("start") int start, @Param("pageSize") int pageSize);

    /**
     * 统计用户总数
     *
     * @return 用户总数
     */
    @Select("SELECT COUNT(*) FROM user")
    int countAll();

    /**
     * 按 ID 删除用户（账号注销）
     *
     * @param id 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    /**
     * 更新用户角色（管理员提升/撤销）
     *
     * @param id   用户ID
     * @param role 新角色（user/admin）
     * @return 影响行数
     */
    @Update("UPDATE user SET role = #{role} WHERE id = #{id}")
    int updateRole(@Param("id") Integer id, @Param("role") String role);

    /**
     * 登录失败次数加一
     *
     * @param id 用户ID
     * @return 影响行数
     */
    @Update("UPDATE user SET failed_attempts = failed_attempts + 1 WHERE id = #{id}")
    int incrementFailedAttempts(@Param("id") Integer id);

    /**
     * 锁定用户（设置锁定截止时间并清零失败次数）
     *
     * @param id           用户ID
     * @param lockedUntil  锁定截止时间
     * @return 影响行数
     */
    @Update("UPDATE user SET locked_until = #{lockedUntil}, failed_attempts = 0 WHERE id = #{id}")
    int lockUser(@Param("id") Integer id, @Param("lockedUntil") java.time.LocalDateTime lockedUntil);

    /**
     * 清除登录失败记录与锁定（登录成功后调用）
     *
     * @param id 用户ID
     * @return 影响行数
     */
    @Update("UPDATE user SET failed_attempts = 0, locked_until = NULL WHERE id = #{id}")
    int clearFailedAttempts(@Param("id") Integer id);

    /**
     * 统计管理员数量（防止删除/降级最后一个管理员）
     *
     * @return 管理员数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE role = 'admin'")
    int countAdmins();

}

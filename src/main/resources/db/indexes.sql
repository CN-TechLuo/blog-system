-- ============================================================
-- 数据库索引优化脚本
-- 执行前请确认在正确的数据库中: USE blog_db;
-- ============================================================

-- 1. user 表：username 唯一索引（登录时高频查询）
ALTER TABLE `user` ADD UNIQUE INDEX `idx_username` (`username`);

-- 2. article 表：user_id 索引（作者身份验证、按作者查询文章）
ALTER TABLE `article` ADD INDEX `idx_user_id` (`user_id`);

-- 3. article 表：create_time 索引（分页列表按时间倒序排序）
ALTER TABLE `article` ADD INDEX `idx_create_time` (`create_time`);


-- ============================================================
-- 数据库索引优化脚本
-- 执行前请确认在正确的数据库中: USE blog_db;
-- ============================================================

-- 1. user 表：username 唯一索引（登录时高频查询）
ALTER TABLE `user` ADD UNIQUE INDEX `idx_username` (`username`);

-- 2. user 表：password 列扩容（BCrypt 及未来 Argon2 需要更多空间）
ALTER TABLE `user` MODIFY COLUMN `password` VARCHAR(255) NOT NULL;

-- 3. user 表：refresh token 版本号（改密后失效所有 refresh token）
ALTER TABLE `user` ADD COLUMN `token_version` INT DEFAULT 0 NOT NULL AFTER `password`;
ALTER TABLE `user` ADD COLUMN `avatar_url` VARCHAR(500) DEFAULT NULL AFTER `email`;
ALTER TABLE `user` ADD COLUMN `nickname` VARCHAR(50) DEFAULT NULL AFTER `username`;
-- 17. V5 管理员: role 字段
ALTER TABLE `user` ADD COLUMN `role` VARCHAR(20) DEFAULT 'user' AFTER `phone`;
-- 管理员账号通过应用启动时自动提升（admin.bootstrap-username 配置，默认 username=admin），
-- 需先手动注册该用户名（密码自行设置，勿用弱口令）

-- 4. article 表：user_id 索引（作者身份验证、按作者查询文章）
ALTER TABLE `article` ADD INDEX `idx_user_id` (`user_id`);

-- 5. article 表：create_time 索引（分页列表按时间倒序排序）
ALTER TABLE `article` ADD INDEX `idx_create_time` (`create_time`);

-- 6. comment 表：评论功能建表 + 索引
CREATE TABLE IF NOT EXISTS `comment` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `article_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `content` TEXT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_article_id` (`article_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 8. V3 社交互动表：点赞
CREATE TABLE IF NOT EXISTS `article_like` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `article_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_user_article` (`user_id`, `article_id`),
  INDEX `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章点赞表';

-- 9. V3 社交互动表：收藏
CREATE TABLE IF NOT EXISTS `bookmark` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `article_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_user_article` (`user_id`, `article_id`),
  INDEX `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章收藏表';

-- 10. V3 社交互动表：关注
CREATE TABLE IF NOT EXISTS `follow` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `follower_id` INT NOT NULL,
  `followee_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_follow` (`follower_id`, `followee_id`),
  INDEX `idx_followee` (`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- 11. V3 通知表
CREATE TABLE IF NOT EXISTS `notification` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `from_user_id` INT DEFAULT NULL,
  `article_id` INT DEFAULT NULL,
  `comment_id` INT DEFAULT NULL,
  `content` VARCHAR(500) DEFAULT NULL,
  `is_read` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_read` (`user_id`, `is_read`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 12. V3 标签表
CREATE TABLE IF NOT EXISTS `tag` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL,
  `article_count` INT DEFAULT 0,
  UNIQUE INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 13. V3 文章-标签关联表
CREATE TABLE IF NOT EXISTS `article_tag` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `article_id` INT NOT NULL,
  `tag_id` INT NOT NULL,
  UNIQUE INDEX `uk_article_tag` (`article_id`, `tag_id`),
  INDEX `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

-- 14. V3 评论表新增 parent_id 支持嵌套回复 + like_count
ALTER TABLE `comment` ADD COLUMN `parent_id` INT DEFAULT NULL AFTER `content`;
ALTER TABLE `comment` ADD COLUMN `like_count` INT DEFAULT 0 AFTER `parent_id`;
ALTER TABLE `comment` ADD INDEX `idx_parent_id` (`parent_id`);

-- 15. V3 文章表新增社交计数字段
ALTER TABLE `article` ADD COLUMN `like_count` INT DEFAULT 0 AFTER `view_count`;
ALTER TABLE `article` ADD COLUMN `bookmark_count` INT DEFAULT 0 AFTER `like_count`;
ALTER TABLE `article` ADD COLUMN `comment_count` INT DEFAULT 0 AFTER `bookmark_count`;
ALTER TABLE `article` ADD COLUMN `share_count` INT DEFAULT 0 AFTER `comment_count`;

-- 16. V4 多媒体: 封面
ALTER TABLE `article` ADD COLUMN `cover_url` VARCHAR(500) DEFAULT NULL AFTER `share_count`;

-- ============================================================
-- 17. 孤儿数据清理（历史遗留：管理员删除用户后未级联清理的数据）
-- 执行前建议备份: mysqldump -u root -p blog_db > backup.sql
-- ============================================================

-- 17.1 删除作者已不存在的文章
DELETE FROM `article` WHERE user_id NOT IN (SELECT id FROM `user`);

-- 17.2 删除评论：评论者或文章已不存在
DELETE FROM `comment` WHERE user_id NOT IN (SELECT id FROM `user`)
   OR article_id NOT IN (SELECT id FROM `article`);

-- 17.3 删除点赞：用户或文章已不存在
DELETE FROM `article_like` WHERE user_id NOT IN (SELECT id FROM `user`)
   OR article_id NOT IN (SELECT id FROM `article`);

-- 17.4 删除收藏：用户或文章已不存在
DELETE FROM `bookmark` WHERE user_id NOT IN (SELECT id FROM `user`)
   OR article_id NOT IN (SELECT id FROM `article`);

-- 17.5 删除关注关系：任一用户已不存在
DELETE FROM `follow` WHERE follower_id NOT IN (SELECT id FROM `user`)
   OR followee_id NOT IN (SELECT id FROM `user`);

-- 17.6 删除通知：接收者或触发者已不存在，或文章已删除
DELETE FROM `notification` WHERE user_id NOT IN (SELECT id FROM `user`)
   OR from_user_id NOT IN (SELECT id FROM `user`)
   OR (article_id IS NOT NULL AND article_id NOT IN (SELECT id FROM `article`));

-- 17.7 删除反馈：用户已不存在
DELETE FROM `feedback` WHERE user_id NOT IN (SELECT id FROM `user`);

-- ============================================================
-- 18. V6 密码重置令牌表（找回密码）
-- ============================================================
CREATE TABLE IF NOT EXISTS `password_reset_token` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `token_hash` VARCHAR(64) NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `used` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_token` (`token_hash`),
  INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置令牌表';


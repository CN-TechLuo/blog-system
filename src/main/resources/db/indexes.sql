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

-- 7. feedback 表：用户反馈功能
CREATE TABLE IF NOT EXISTS `feedback` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `status` VARCHAR(20) DEFAULT 'pending',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';


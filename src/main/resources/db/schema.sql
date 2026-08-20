-- ============================================================
-- blog-system 完整数据库初始化脚本（空库一键执行，幂等可重复运行）
-- 用法: mysql -u root -p < schema.sql
-- 或:   USE blog_db; SOURCE schema.sql;
-- ============================================================
CREATE DATABASE IF NOT EXISTS `blog_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `blog_db`;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(20) NOT NULL,
  `nickname` VARCHAR(50) DEFAULT NULL,
  `password` VARCHAR(255) NOT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `phone` VARCHAR(11) DEFAULT NULL,
  `role` VARCHAR(20) DEFAULT 'user',
  `failed_attempts` INT DEFAULT 0 NOT NULL,
  `locked_until` DATETIME DEFAULT NULL,
  `token_version` INT DEFAULT 0 NOT NULL,
  `avatar_url` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `idx_username` (`username`),
  UNIQUE INDEX `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 管理员账号通过应用启动时自动提升（admin.bootstrap-username 配置，默认 username=admin），
-- 需先手动注册该用户名（密码自行设置，勿用弱口令）

-- ============================================================
-- 2. 文章表
-- ============================================================
CREATE TABLE IF NOT EXISTS `article` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `user_id` INT NOT NULL,
  `view_count` INT DEFAULT 0,
  `like_count` INT DEFAULT 0,
  `bookmark_count` INT DEFAULT 0,
  `comment_count` INT DEFAULT 0,
  `share_count` INT DEFAULT 0,
  `hot_score` DOUBLE DEFAULT 0 NOT NULL,
  `cover_url` VARCHAR(500) DEFAULT NULL,
  `ai_generated` TINYINT DEFAULT 0 NOT NULL COMMENT 'AI 生成标识 0=否 1=是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_create_time` (`create_time`),
  INDEX `idx_create_time_id` (`create_time`, `id`),
  INDEX `idx_hot_score` (`hot_score`),
  FULLTEXT INDEX `ft_title` (`title`) WITH PARSER ngram,
  CONSTRAINT `fk_article_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- ============================================================
-- 3. 评论表（支持嵌套回复）
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `article_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `content` TEXT NOT NULL,
  `parent_id` INT DEFAULT NULL,
  `like_count` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_article_id` (`article_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_parent_id` (`parent_id`),
  CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`),
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `comment`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ============================================================
-- 4. 反馈表
-- ============================================================
CREATE TABLE IF NOT EXISTS `feedback` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `status` VARCHAR(20) DEFAULT 'pending',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- ============================================================
-- 5. 点赞表
-- ============================================================
CREATE TABLE IF NOT EXISTS `article_like` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `article_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_user_article` (`user_id`, `article_id`),
  INDEX `idx_article_id` (`article_id`),
  CONSTRAINT `fk_article_like_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_article_like_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章点赞表';

-- ============================================================
-- 6. 收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS `bookmark` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `article_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_user_article` (`user_id`, `article_id`),
  INDEX `idx_article_id` (`article_id`),
  CONSTRAINT `fk_bookmark_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_bookmark_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章收藏表';

-- ============================================================
-- 7. 关注表
-- ============================================================
CREATE TABLE IF NOT EXISTS `follow` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `follower_id` INT NOT NULL,
  `followee_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_follow` (`follower_id`, `followee_id`),
  INDEX `idx_followee` (`followee_id`),
  CONSTRAINT `fk_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_follow_followee` FOREIGN KEY (`followee_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- ============================================================
-- 8. 通知表
-- ============================================================
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
  INDEX `idx_user_read` (`user_id`, `is_read`, `create_time`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_notification_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `user`(`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_notification_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notification_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ============================================================
-- 9. 标签表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tag` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL,
  `article_count` INT DEFAULT 0,
  UNIQUE INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- ============================================================
-- 10. 文章-标签关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `article_tag` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `article_id` INT NOT NULL,
  `tag_id` INT NOT NULL,
  UNIQUE INDEX `uk_article_tag` (`article_id`, `tag_id`),
  INDEX `idx_tag` (`tag_id`),
  CONSTRAINT `fk_article_tag_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`),
  CONSTRAINT `fk_article_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

-- ============================================================
-- 11. 密码重置令牌表
-- ============================================================
CREATE TABLE IF NOT EXISTS `password_reset_token` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `token_hash` VARCHAR(64) NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `used` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_token` (`token_hash`),
  INDEX `idx_user` (`user_id`),
  CONSTRAINT `fk_password_reset_token_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置令牌表';

-- ============================================================
-- 12. 站点配置表（单行：id=1）
-- ============================================================
CREATE TABLE IF NOT EXISTS `site_config` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `contact_email` VARCHAR(128) NOT NULL DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点配置表';

INSERT INTO `site_config` (`id`, `contact_email`) VALUES (1, '')
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ============================================================
-- 13. JWT 黑名单（登出吊销）
-- ============================================================
CREATE TABLE IF NOT EXISTS `token_blacklist` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `jti` VARCHAR(64) NOT NULL,
  `expire_time` DATETIME NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_jti` (`jti`),
  INDEX `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JWT 黑名单';

-- ============================================================
-- 14. AI 调用用量记录
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_usage` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `api_type` VARCHAR(32) NOT NULL,
  `input_chars` INT DEFAULT 0,
  `output_chars` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_time` (`user_id`, `create_time`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用用量记录';

-- ============================================================
-- 15. 用户举报表（投诉举报闭环）
-- ============================================================
CREATE TABLE IF NOT EXISTS `report` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `reporter_id` INT NOT NULL,
  `target_type` VARCHAR(16) NOT NULL COMMENT '举报对象: article | comment',
  `target_id` INT NOT NULL,
  `reason` VARCHAR(32) NOT NULL COMMENT '举报原因分类',
  `detail` VARCHAR(500) DEFAULT NULL COMMENT '补充说明',
  `status` VARCHAR(16) DEFAULT 'pending' COMMENT 'pending=待处理 resolved=已处理',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_status_time` (`status`, `create_time`),
  INDEX `idx_target` (`target_type`, `target_id`),
  INDEX `idx_reporter` (`reporter_id`),
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户举报表';

-- ============================================================
-- 初始化完成
-- ============================================================

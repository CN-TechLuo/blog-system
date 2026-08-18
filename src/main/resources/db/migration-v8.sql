-- ============================================================
-- migration-v8: 站点配置表（管理员联系邮箱，由管理员自定义）
-- 一次性迁移，适用于存量数据库；全新环境直接执行 schema.sql
-- 用法: mysql -u root -p blog_db < migration-v8.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS `site_config` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `contact_email` VARCHAR(128) NOT NULL DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点配置表';

INSERT INTO `site_config` (`id`, `contact_email`) VALUES (1, '')
ON DUPLICATE KEY UPDATE `id` = `id`;

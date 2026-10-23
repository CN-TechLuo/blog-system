#!/bin/bash
# ============================================================
# MySQL 初始化：创建最小权限业务账号与备份账号
# 由官方 mysql 镜像的 /docker-entrypoint-initdb.d 自动执行一次
# ============================================================
set -euo pipefail

if [ -z "${DB_APP_PASSWORD:-}" ] || [ -z "${DB_BACKUP_PASSWORD:-}" ]; then
  echo "[mysql-init] 未配置 DB_APP_PASSWORD/DB_BACKUP_PASSWORD，跳过最小权限账号创建（沿用 root）"
  exit 0
fi

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'blog_app'@'%' IDENTIFIED BY '${DB_APP_PASSWORD}';
ALTER USER 'blog_app'@'%' IDENTIFIED BY '${DB_APP_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES,
      CREATE VIEW, SHOW VIEW, TRIGGER, EXECUTE ON blog_db.* TO 'blog_app'@'%';

CREATE USER IF NOT EXISTS 'blog_backup'@'%' IDENTIFIED BY '${DB_BACKUP_PASSWORD}';
ALTER USER 'blog_backup'@'%' IDENTIFIED BY '${DB_BACKUP_PASSWORD}';
GRANT SELECT, LOCK TABLES, SHOW VIEW, EVENT, TRIGGER ON blog_db.* TO 'blog_backup'@'%';

FLUSH PRIVILEGES;
SQL

echo "[mysql-init] blog_app / blog_backup 已创建"

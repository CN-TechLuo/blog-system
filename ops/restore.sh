#!/bin/bash
# ============================================================
# 备份恢复脚本
# 用法: ./restore.sh /path/to/backup.sql.gz
# 警告: 会覆盖当前数据库数据，恢复前请再次确认
# ============================================================
set -euo pipefail

BACKUP_FILE="${1:?用法: ./restore.sh /path/to/backup.sql.gz}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:?请设置 DB_PASSWORD 环境变量}"
DB_NAME="${DB_NAME:-blog_db}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "备份文件不存在: $BACKUP_FILE"
  exit 1
fi

gzip -t "$BACKUP_FILE"
echo "[$(date '+%F %T')] 恢复 $BACKUP_FILE -> $DB_NAME"
gunzip -c "$BACKUP_FILE" | mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME"
echo "[$(date '+%F %T')] 恢复完成"

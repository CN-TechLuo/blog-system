#!/bin/bash
# ============================================================
# 数据库自动备份脚本（crontab 每日执行）
# 用法: ./backup.sh [/path/to/backups]
# 依赖: mysqldump, gzip（docker 内执行或宿主机执行均可）
# 特性: 按天命名、30 天轮转、备份后完整性校验
# crontab 示例（宿主机，每天 02:30）:
#   30 2 * * * DB_PASSWORD=xxx /opt/blog/ops/backup.sh /opt/blog/backups >> /opt/blog/logs/backup.log 2>&1
# ============================================================
set -euo pipefail

BACKUP_DIR="${1:-/backups}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:?请设置 DB_PASSWORD 环境变量}"
DB_NAME="${DB_NAME:-blog_db}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

mkdir -p "$BACKUP_DIR"
STAMP=$(date +%Y%m%d_%H%M%S)
FILE="$BACKUP_DIR/${DB_NAME}_${STAMP}.sql.gz"

echo "[$(date '+%F %T')] 开始备份 $DB_NAME -> $FILE"
mysqldump -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" \
  --single-transaction --quick --routines --triggers "$DB_NAME" | gzip > "$FILE"

# 完整性校验：备份文件可被 gzip 正常解压
gzip -t "$FILE"
SIZE=$(du -h "$FILE" | cut -f1)
echo "[$(date '+%F %T')] 备份完成（${SIZE}），校验通过"

# 轮转：删除 N 天前的备份
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +"$RETENTION_DAYS" -delete
echo "[$(date '+%F %T')] 轮转完成（保留 ${RETENTION_DAYS} 天）"

#!/bin/bash
# ============================================================
# 加密备份恢复脚本
# 用法: DB_PASSWORD=xxx BACKUP_KEY_FILE=/run/secrets/backup_key ./restore.sh /backup/blog_db_xxx.sql.gz.enc
# 警告: 会覆盖当前数据库数据，恢复前请再次确认
# ============================================================
set -euo pipefail
umask 077

BACKUP_FILE="${1:?用法: ./restore.sh /backup/blog_db_xxx.sql.gz.enc}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-blog_app}"
DB_PASSWORD="${DB_PASSWORD:?请设置 DB_PASSWORD 环境变量}"
DB_NAME="${DB_NAME:-blog_db}"
BACKUP_KEY_FILE="${BACKUP_KEY_FILE:-/run/secrets/backup_key}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "备份文件不存在: $BACKUP_FILE" >&2
  exit 1
fi
if [ ! -r "$BACKUP_KEY_FILE" ]; then
  echo "备份密钥文件不可读: $BACKUP_KEY_FILE" >&2
  exit 1
fi

CNF=$(mktemp)
trap 'rm -f "$CNF"' EXIT
cat > "$CNF" <<EOF
[client]
user=$DB_USER
password=$DB_PASSWORD
host=$DB_HOST
port=$DB_PORT
EOF

echo "[$(date '+%F %T')] 恢复 $BACKUP_FILE -> $DB_NAME"
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass file:"$BACKUP_KEY_FILE" -in "$BACKUP_FILE" \
  | gunzip -c \
  | mysql --defaults-extra-file="$CNF" "$DB_NAME"
echo "[$(date '+%F %T')] 恢复完成"

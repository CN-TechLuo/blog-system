#!/bin/bash
# ============================================================
# 数据库自动备份脚本（加密 + 完整性校验 + 30 天轮转）
# 用法: DB_PASSWORD=xxx BACKUP_KEY_FILE=/run/secrets/backup_key ./backup.sh [/backup]
# 依赖: mysqldump, gzip, openssl
# 说明: 密码通过临时 defaults-extra-file 传递，不出现在 ps 参数；
#       备份文件使用 AES-256-CBC + PBKDF2 加密，密钥文件权限必须为 600。
# ============================================================
set -euo pipefail
umask 077

BACKUP_DIR="${1:-/backups}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-blog_backup}"
DB_PASSWORD="${DB_PASSWORD:?请设置 DB_PASSWORD 环境变量}"
DB_NAME="${DB_NAME:-blog_db}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
BACKUP_KEY_FILE="${BACKUP_KEY_FILE:-/run/secrets/backup_key}"

if [ ! -r "$BACKUP_KEY_FILE" ]; then
  echo "[backup] 备份密钥文件不可读: $BACKUP_KEY_FILE" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
STAMP=$(date +%Y%m%d_%H%M%S)
FILE="$BACKUP_DIR/${DB_NAME}_${STAMP}.sql.gz.enc"

CNF=$(mktemp)
trap 'rm -f "$CNF"' EXIT
cat > "$CNF" <<EOF
[client]
user=$DB_USER
password=$DB_PASSWORD
host=$DB_HOST
port=$DB_PORT
EOF

echo "[$(date '+%F %T')] 开始备份 $DB_NAME -> $FILE"
mysqldump --defaults-extra-file="$CNF" --single-transaction --quick --routines --triggers "$DB_NAME" \
  | gzip -c \
  | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -pass file:"$BACKUP_KEY_FILE" > "$FILE"

# 解密后做 gzip 完整性校验，确保密钥与密文匹配
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass file:"$BACKUP_KEY_FILE" -in "$FILE" \
  | gzip -t

SIZE=$(du -h "$FILE" | cut -f1)
echo "[$(date '+%F %T')] 备份完成（${SIZE}），加密与完整性校验通过"

find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz.enc" -mtime +"$RETENTION_DAYS" -delete
echo "[$(date '+%F %T')] 轮转完成（保留 ${RETENTION_DAYS} 天）"

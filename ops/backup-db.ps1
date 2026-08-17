# ============================================================
# 博客系统数据库 + 上传目录备份脚本
# 用法: powershell -ExecutionPolicy Bypass -File ops\backup-db.ps1
# 定时任务(Windows): schtasks /Create /TN BlogBackup /SC DAILY /ST 03:00
#   /TR "powershell -ExecutionPolicy Bypass -File D:\IDE\xiangmu\blog-system\ops\backup-db.ps1"
# ============================================================

$ErrorActionPreference = "Stop"

# 备份目录（可修改）
$BACKUP_DIR = Join-Path (Split-Path -Parent $PSScriptRoot) "backup"
# 数据库凭据（推荐使用 MYSQL_PWD 环境变量而非命令行明文密码）
$DB_NAME     = if ($env:BACKUP_DB_NAME)     { $env:BACKUP_DB_NAME }     else { "blog_db" }
$DB_USER     = if ($env:BACKUP_DB_USER)     { $env:BACKUP_DB_USER }     else { "root" }
# 保留天数
$RETENTION_DAYS = if ($env:BACKUP_RETENTION_DAYS) { [int]$env:BACKUP_RETENTION_DAYS } else { 14 }
$UPLOAD_DIR = Join-Path (Split-Path -Parent $PSScriptRoot) "uploads"

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
New-Item -ItemType Directory -Path $BACKUP_DIR -Force | Out-Null

$dumpFile = Join-Path $BACKUP_DIR "blog_db_$stamp.sql.gz"
$uploadZip = Join-Path $BACKUP_DIR "uploads_$stamp.zip"

Write-Host "[Backup] 开始备份数据库到 $dumpFile"

if ($env:MYSQL_PWD) {
    $env:MYSQL_PWD = $env:MYSQL_PWD
} elseif ($env:BACKUP_DB_PASSWORD) {
    $env:MYSQL_PWD = $env:BACKUP_DB_PASSWORD
} else {
    Write-Error "未提供数据库密码。请设置环境变量 BACKUP_DB_PASSWORD 或 MYSQL_PWD"
}

$dumpSql = Join-Path $BACKUP_DIR "blog_db_$stamp.sql"
& mysqldump -u $DB_USER --single-transaction --default-character-set=utf8mb4 --routines --events $DB_NAME --result-file=$dumpSql
if ($LASTEXITCODE -ne 0) { Write-Error "mysqldump 失败" }

# 压缩（gzip 不可用时降级为保留原 SQL）
$gzip = Get-Command gzip -ErrorAction SilentlyContinue
if ($gzip) {
    & gzip -f $dumpSql
    $dumpFile = "$dumpSql.gz"
} else {
    $dumpFile = $dumpSql
    Write-Host "[Backup] 未找到 gzip，保留未压缩 SQL"
}

Write-Host "[Backup] 开始备份上传目录 $UPLOAD_DIR"
if (Test-Path $UPLOAD_DIR) {
    Compress-Archive -Path (Join-Path $UPLOAD_DIR "*") -DestinationPath $uploadZip -Force
} else {
    Write-Host "[Backup] 上传目录不存在，跳过"
}

# 清理过期备份
$cutoff = (Get-Date).AddDays(-$RETENTION_DAYS)
Get-ChildItem -Path $BACKUP_DIR -File | Where-Object { $_.LastWriteTime -lt $cutoff } | Remove-Item -Force
Write-Host "[Backup] 完成。保留最近 $RETENTION_DAYS 天备份。"

# 生产部署指南（DEPLOY）

本文档覆盖从开发到企业级生产的完整部署流程，对应 16 级安全体系中 L15/L16 的运维侧要求。

## 0. 部署架构

```
用户 → Nginx(TLS/WAF/限流) → 后端 8080 → MySQL 8
         │
         └── 前端静态资源(dist)
```

## 1. 上线前检查清单

- [ ] 域名已备案（中国大陆服务器必须）
- [ ] HTTPS 证书已配置（Let's Encrypt / 云厂商证书）
- [ ] 生产密钥全部通过环境变量注入（JWT_SECRET ≥32 字节、DB 密码、DEEPSEEK_API_KEY）
- [ ] SMTP 已配置并**实测**找回密码邮件可达
- [ ] 内容审核词库（`sensitive-words.txt`）已按业务补充
- [ ] 站点管理员邮箱已在管理后台配置
- [ ] 用户协议/隐私政策页面已发布，注册页勾选同意
- [ ] 监控告警已接入（Prometheus 抓取 `/actuator/prometheus`）

## 2. 构建与发布

```bash
# 后端（产物 target/blog-system-0.0.1-SNAPSHOT.jar）
mvn clean package -DskipTests

# 前端（产物 dist/）
cd blog-frontend && npm ci && npm run build

# 版本发布（打 tag 触发 CI 自动构建镜像并推送 GHCR）
git tag -a v1.0.0 -m "release v1.0.0"
git push origin v1.0.0
```

## 3. 生产部署（Docker Compose）

```bash
# 填写密钥
cp .env.example .env   # 编辑：DB_PASSWORD / JWT_SECRET / DEEPSEEK_API_KEY / CORS_ORIGINS
openssl rand -base64 32   # 生成 JWT_SECRET

docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml logs -f backend
```

后端容器已做运行时加固：非 root、`read_only` + `cap_drop ALL`、`no-new-privileges`。

## 4. Nginx（TLS + WAF + 限流）

`ops/nginx.conf` 模板包含：强制 HTTPS、HSTS、`client_max_body_size 6m`、
`limit_req`（10r/s 突发 20）、`/actuator` 仅内网可达、上传目录静态服务。

## 5. 数据备份与恢复

```bash
# 每日备份（建议配合计划任务：Windows 任务计划 / Linux crontab）
# crontab 示例（Linux）: 0 3 * * * /app/ops/backup-db.sh /backup
powershell -File ops/backup-db.ps1 -BackupDir D:\backups

# 备份文件必须加密后异地存储（Linux 示例）：
tar czf - /backup | openssl enc -aes-256-cbc -salt -pbkdf2 -pass file:/root/.backup-pass -out backup-$(date +%F).tar.gz.enc

# 恢复演练（至少每季度一次）：
mysql -u root -p blog_db < backup-xxxx.sql
```

## 6. 监控告警

- Prometheus 抓取 `http://<内网>:8080/actuator/prometheus`（JVM/HTTP/缓存指标）。
- 必配告警：登录失败激增（audit.log 中 LOGIN_FAIL 计数）、429 突增、5xx 比例 >1%、磁盘/内存水位。
- 日志：`logs/app.log`（业务）、`logs/audit.log`（安全审计，管理后台可查询）。

## 7. 上线后运维手册

| 事项 | 频率 | 说明 |
|---|---|---|
| 密钥轮换 JWT_SECRET | 每季度 | 轮换后全员需重新登录 |
| DeepSeek Key 用量检查 | 每周 | 管理后台「AI 用量」页 + 成本告警 |
| 备份恢复演练 | 每季度 | 验证备份可用性 |
| 依赖 CVE 扫描 | 每月 | Dependabot 告警 + `mvn dependency-check` |
| 敏感词库更新 | 按需 | 新增违规词后重启生效 |

## 8. 应急响应

1. 发现密钥/令牌泄露 → 立即轮换 `JWT_SECRET`、DB 密码、DeepSeek Key、SMTP 密码。
2. 发现违规内容 → 管理后台删除文章/用户；补充敏感词库。
3. 疑似账号攻击 → 查看管理后台「审计日志」，确认锁定与限流生效。
4. 数据损坏 → 按第 5 节执行恢复。

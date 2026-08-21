# 生产部署指南（DEPLOY）

本文档覆盖从开发到企业级生产的完整部署流程，对应 16 级安全体系中 L15/L16 的运维侧要求。

## 0. 部署架构

```
用户 → Nginx(TLS/WAF/限流) → 后端(可多副本) → MySQL 8
         │                        │
         │                        ├── Redis（限流/多实例共享）
         │                        └── Prometheus ← Alertmanager → 邮件/Webhook
         └── 前端静态资源(dist)
```

## 1. 上线前检查清单（合规 + 技术门禁，全部勾选后方可公网上线）

### 合规类（缺一不可）

- [ ] 域名已备案（ICP）+ 公安备案（公安互联网站备案）
- [ ] 企业/主体资质：营业执照、等保二级测评（按所在地监管要求）
- [ ] HTTPS 证书已配置（Let's Encrypt / 云厂商证书）
- [ ] 用户协议/隐私政策页面已发布，注册页勾选同意；隐私政策已声明 **AI 数据处理与第三方大模型调用**
- [ ] 用户协议已包含 **侵权投诉（通知-删除）条款** 与 **AI 生成内容责任条款**
- [ ] 生成式 AI 服务合规评估/备案核查已向属地网信办确认（接入大模型面向公众提供服务）
- [ ] 个人信息保护影响评估（PIA）已完成（收集手机号/邮箱）
- [ ] AI 生成内容标识已生效（文章发布页勾选「含 AI 生成内容」，详情页显著展示"AI 生成"）
- [ ] 账号注销（个人中心 → 注销账号）与数据导出（个人中心 → 导出数据）已实测通过
- [ ] 投诉举报闭环已实测：文章/评论举报 → 管理后台「举报管理」处置
- [ ] 日志留存 ≥180 天（logback 已配置，需确认磁盘容量与备份策略）
- [ ] 未成年人保护与投诉受理渠道在协议中说明

### 技术类（缺一不可）

- [ ] 生产密钥全部通过环境变量注入（JWT_SECRET ≥32 字节、DB 密码、DEEPSEEK_API_KEY），`.env` 不入库
- [ ] SMTP 已配置并**实测**找回密码邮件可达（可用 `docker compose --profile smtp-test up maildev` 验证链路）
- [ ] 图形验证码已启用（`CAPTCHA_ENABLED=true`），注册/登录实测校验
- [ ] 多实例部署时 `RATE_LIMIT_STORE=redis` 且 redis 服务健康（单机可保持 memory）
- [ ] Prometheus + Alertmanager 已接入并配置告警接收（邮件/Webhook），告警规则实测触发
- [ ] 每日自动备份已运行（compose 内置 backup 服务或宿主机 crontab + `ops/backup.sh`），并完成一次**恢复演练**
- [ ] 压测通过：`k6 run --vus 50 --duration 60s ops/k6/load-test.js`（p95 < 500ms、错误率 < 1%）
- [ ] 渗透/安全自测完成：登录锁定、JWT 吊销、XSS、敏感词、越权（非 admin 访问管理接口 403）逐项复测
- [ ] 内容审核词库（`sensitive-words.txt`）已按业务补充
- [ ] 站点管理员邮箱已在管理后台配置
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
# 方式一：compose 内置备份服务（每日 02:30，保留 30 份，见 docker-compose.prod.yml backup 服务）
# 方式二：宿主机脚本 + crontab
# crontab 示例（Linux）: 30 2 * * * DB_PASSWORD=xxx /opt/blog/ops/backup.sh /opt/blog/backups >> /opt/blog/logs/backup.log 2>&1
# 恢复: ./ops/restore.sh /backups/blog_db_20260820_023000.sql.gz

# Windows PowerShell（宿主机）:
powershell -File ops/backup-db.ps1 -BackupDir D:\backups

# 备份文件必须加密后异地存储（Linux 示例）：
tar czf - /backup | openssl enc -aes-256-cbc -salt -pbkdf2 -pass file:/root/.backup-pass -out backup-$(date +%F).tar.gz.enc

# 恢复演练（至少每季度一次）：
./ops/restore.sh backup-xxxx.sql.gz
```

## 6. 监控告警

- Prometheus 抓取 `http://<内网>:8080/actuator/prometheus`（JVM/HTTP/缓存指标）。
- 告警规则（`ops/prometheus/alerts.yml`）：实例宕机、5xx>5%、P95>2s、堆内存>90%、连接池排队、429 突增。
- Alertmanager 接收（`ops/alertmanager/alertmanager.yml`）：邮件 + Webhook（替换为钉钉/飞书/企业微信告警群）。
- 日志：`logs/blog-system.log`（业务，留存 90 天）、`logs/audit.log`（安全审计，留存 180 天，管理后台可查询）。

## 6.1 SMTP 链路测试（上线前必须执行）

```bash
# 启动测试邮箱服务（容器内 SMTP:1025，Web 界面:1080）
docker compose -f docker-compose.prod.yml --profile smtp-test up -d maildev

# 本地后端临时配置测试 SMTP（或直接给 backend 容器加环境变量）:
#   SPRING_MAIL_HOST=localhost SPRING_MAIL_PORT=1025 SPRING_MAIL_USERNAME= SPRING_MAIL_PASSWORD=
# 触发「忘记密码」→ 打开 http://localhost:1080 查看邮件是否到达 → 完成重置密码全链路
# 验证完成后切换为真实 SMTP 并再次实测
```

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

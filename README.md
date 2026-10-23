# 智能博客系统 (blog-system)

基于 Spring Boot 4.1 + MyBatis + MySQL 8 + Vue 3 的企业级博客平台，集成 DeepSeek AI 助手、社交互动、评论通知、管理后台等功能。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 17, Spring Boot 4.1.0, MyBatis, MySQL 8.0, JWT (jjwt 0.12), BCrypt, SpringDoc |
| 前端 | Vue 3, Vite 8, Element Plus, Axios, DOMPurify |
| AI | DeepSeek API（对话/写作/润色/摘要/标签/代码/翻译/SEO/校对 等 12 种模式） |
| 部署 | Docker 多阶段构建, 非 root 运行, GitHub Actions CI |

> CI 需要在仓库 Secrets 中配置 `CI_DB_PASSWORD` 与 `CI_JWT_SECRET`；缺失时构建会 fail-closed 失败，仓库内不再保留可解码的密钥回退值。

## 快速启动（开发环境）

```bash
# 1. 创建空库（表结构由应用启动时的 Flyway V1–V4 自动创建/升级）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS blog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 2. 设置环境变量（PowerShell）
$env:DB_USERNAME="root"; $env:DB_PASSWORD="你的密码"; $env:JWT_SECRET="你的随机密钥"

# 3. 启动后端（Flyway 自动建表并升级到最新版本）
./mvnw spring-boot:run

# 4. 启动前端
cd ../blog-frontend && npm install && npm run dev
```

> `src/main/resources/db/schema.sql` 是历史全量快照，仅用于人工查看；它与 V2+ 增量迁移存在列重叠。空库请让 Flyway 自动建表，不要先执行 `schema.sql` 再启动应用。
>
> 由旧版 `schema.sql` 初始化的存量库（已含 V2/V3 的 `failed_attempts`、`ai_generated` 等列）首次接入 Flyway 时，需设置 `SPRING_FLYWAY_BASELINE_VERSION=3`（或手工写入 `flyway_schema_history`），否则 V2/V3 的 `ADD COLUMN` 会因列已存在而失败。

## 环境变量清单

| 变量 | 必填 | 说明 | 示例 |
|------|:--:|------|------|
| `SPRING_DATASOURCE_URL` | 否* | JDBC 连接串 | `jdbc:mysql://localhost:3306/blog_db?useSSL=true&serverTimezone=Asia/Shanghai` |
| `DB_USERNAME` | ✅ | 数据库用户名（compose 默认 `root`；最小权限账号可选） | `root` |
| `DB_PASSWORD` | ✅ | 数据库密码 | — |
| `DB_APP_PASSWORD` | 可选 | 启用最小权限账号后使用（见 `ops/mysql-init`） | — |
| `DB_BACKUP_PASSWORD` | 可选 | 启用最小权限账号后使用 | — |
| `JWT_SECRET` | ✅ | JWT 签名密钥（≥32字节随机数 base64） | 生成：`openssl rand -base64 32` |
| `DEEPSEEK_API_KEY` | 否 | DeepSeek API Key（缺省时 AI 功能降级提示） | `sk-xxx` |
| `CORS_ORIGINS` | 生产建议 | 允许的前端域名（逗号分隔，禁止 `*`） | `https://blog.example.com` |
| `TRUSTED_PROXIES` | 反代必填 | 可信代理 CIDR（逗号分隔） | `172.16.0.0/12` 或 `127.0.0.1/32` |
| `SERVER_PORT` | 否 | 端口（默认 8080） | `8080` |
| `ADMIN_BOOTSTRAP_TOKEN` | 首次部署 | 一次性管理员引导令牌 | 随机 32+ 字符 |
| `RESET_PASSWORD_URL` | 否 | 找回密码链接前缀 | `https://blog.example.com/reset-password` |

\* 未设置时回退本地 localhost（仅开发用途，生产必须显式设置）

## 功能特性

- 文章发布/编辑/删除、Markdown、FULLTEXT 全文搜索、热榜、封面图上传
- 点赞/收藏/关注、评论、通知中心
- DeepSeek AI 助手：流式对话（多轮记忆）+ 写作/润色/摘要/标签/代码/翻译/SEO/校对等 12 种模式
- 找回密码（重置令牌）、双 Token 刷新、管理后台（用户/文章/反馈管理）

## 管理员初始化

系统**无内置管理员密码**，也不会按用户名自动提权。设置首个管理员：

1. 注册一个普通账号，记录其用户 ID。
2. 设置 `ADMIN_BOOTSTRAP_TOKEN` 环境变量并重启应用。
3. 调用一次性引导接口（系统已有管理员后自动关闭）：
   ```bash
   curl -X POST https://你的域名/api/bootstrap/admin \
     -H "X-Bootstrap-Token: $ADMIN_BOOTSTRAP_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"userId": 1}'
   ```
4. 引导成功后清空 `ADMIN_BOOTSTRAP_TOKEN` 并重启。

登录后头部出现 🛡️ 管理入口，可管理用户/文章/反馈。

## 生产部署（Docker）

```bash
# 构建镜像
docker build -t blog-system:latest .

# 运行（仅绑定本机回环，外部访问必须经 Nginx TLS 反向代理）
docker run -d --name blog \
  -p 127.0.0.1:8080:8080 \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=你的数据库密码 \
  -e JWT_SECRET=你的JWT密钥 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://db-host:3306/blog_db?useSSL=true&serverTimezone=Asia/Shanghai \
  -e DEEPSEEK_API_KEY=sk-xxx \
  -e CORS_ORIGINS=https://你的前端域名 \
  -v blog-uploads:/app/uploads \
  blog-system:latest
```

镜像内置：`SPRING_PROFILES_ACTIVE=prod`（自动关闭 Swagger）、`TZ=Asia/Shanghai`、健康检查、非 root 运行。后端端口只应绑定 `127.0.0.1`，不要直接暴露 `0.0.0.0:8080`，否则会绕过 Nginx 的 TLS 与 `/actuator` 内网限制。

## 前端生产部署（Nginx）

```bash
# 前端仓库构建
cd blog-frontend && npm install && npm run build   # 产物在 dist/

# nginx.conf 关键配置
server {
    listen 80;
    server_name blog.example.com;
    root /usr/share/nginx/html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        # 必须覆盖客户端自带的 X-Forwarded-For
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    location /uploads/ { proxy_pass http://127.0.0.1:8080; }
    location /        { try_files $uri /index.html; }   # SPA 路由兜底
}
```

后端需设置 `TRUSTED_PROXIES` 为 Nginx 的来源网段（Docker 默认 `172.16.0.0/12`，宿主机 Nginx 用 `127.0.0.1/32`），否则应用不会采信转发头。

## 数据库备份

```bash
# 生成备份加密密钥（只生成一次，务必异地保存）
openssl rand -base64 48 > ops/backup.key && chmod 600 ops/backup.key

# 加密备份（AES-256-CBC + PBKDF2，自动保留 30 天）
DB_PASSWORD=你的备份账号密码 BACKUP_KEY_FILE=ops/backup.key bash ops/backup.sh /path/to/backups

# 恢复（会覆盖当前库，先确认）
DB_PASSWORD=你的业务账号密码 BACKUP_KEY_FILE=ops/backup.key \
  bash ops/restore.sh /path/to/backups/blog_db_20261023_023000.sql.gz.enc
```

不要用 `mysqldump > backup.sql` 把全量数据落在仓库目录；`.gitignore` 已忽略 `*.sql` 与备份文件，但仍应把备份放到仓库外并加密。

## 目录结构

```
src/main/resources/db/
├── migration/        # Flyway V1–V4（应用启动时自动执行，权威来源）
├── schema.sql        # 历史全量快照（仅人工参考，不要先于 Flyway 执行）
├── migration-v7.sql  # 存量老库一次性迁移（含空表保护）
└── indexes.sql       # 存量老库的增量变更 + 孤儿数据清理脚本
```

## 安全特性

完整安全体系见 **[SECURITY.md](SECURITY.md)**（16 级纵深防御），核心摘要：

- JWT 双 Token（30 分钟 Access + 7 天 Refresh）+ issuer/audience 校验 + Refresh 旋转
- BCrypt cost=12 + 密码复杂度校验 + 登录限流 + 验证码（不再按账号冻结，避免锁定武器化）
- 全链路 XSS 过滤（标题/内容/评论/昵称）+ DOMPurify 前端消毒
- 管理员 RBAC + 级联删除 + 最后管理员保护
- 手机/邮箱脱敏、CSP/HSTS 安全响应头、CORS 白名单、全局限流
- 独立安全审计日志（logs/audit.log）

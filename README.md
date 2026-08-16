# 智能博客系统 (blog-systme)

基于 Spring Boot 4.1 + MyBatis + MySQL 8 + Vue 3 的企业级博客平台，集成 DeepSeek AI 助手、社交互动、评论通知、管理后台等功能。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 17, Spring Boot 4.1.0, MyBatis, MySQL 8.0, JWT (jjwt 0.12), BCrypt, SpringDoc |
| 前端 | Vue 3, Vite 8, Element Plus, Axios, DOMPurify |
| AI | DeepSeek API（对话/写作/润色/摘要/标签/代码/翻译/SEO/校对 等 12 种模式） |
| 部署 | Docker 多阶段构建, 非 root 运行, GitHub Actions CI |

## 快速启动（开发环境）

```bash
# 1. 初始化数据库（幂等，可重复执行）
mysql -u root -p < src/main/resources/db/schema.sql

# 2. 设置环境变量（PowerShell）
$env:DB_USERNAME="root"; $env:DB_PASSWORD="你的密码"; $env:JWT_SECRET="你的随机密钥"

# 3. 启动后端
./mvnw spring-boot:run

# 4. 启动前端
cd ../blog-frontend && npm install && npm run dev
```

## 环境变量清单

| 变量 | 必填 | 说明 | 示例 |
|------|:--:|------|------|
| `SPRING_DATASOURCE_URL` | 否* | JDBC 连接串 | `jdbc:mysql://localhost:3306/blog_db?useSSL=true&serverTimezone=Asia/Shanghai` |
| `DB_USERNAME` | ✅ | 数据库用户名 | `root` |
| `DB_PASSWORD` | ✅ | 数据库密码 | — |
| `JWT_SECRET` | ✅ | JWT 签名密钥（≥32字节随机数 base64） | 生成：`openssl rand -base64 32` |
| `DEEPSEEK_API_KEY` | 否 | DeepSeek API Key（缺省时 AI 功能降级提示） | `sk-xxx` |
| `CORS_ORIGINS` | 生产建议 | 允许的前端域名（逗号分隔） | `https://blog.example.com` |
| `SERVER_PORT` | 否 | 端口（默认 8080） | `8080` |
| `ADMIN_BOOTSTRAP_ENABLED` | 否 | 允许首启自动提升管理员（默认 `false`，生产保持关闭） | `true` |
| `ADMIN_BOOTSTRAP_USERNAME` | 否 | 首个管理员用户名（默认 `admin`） | `admin` |
| `RESET_PASSWORD_URL` | 否 | 找回密码链接前缀 | `https://blog.example.com/reset-password` |

\* 未设置时回退本地 localhost（仅开发用途，生产必须显式设置）

## 功能特性

- 文章发布/编辑/删除、Markdown、FULLTEXT 全文搜索、热榜、封面图上传
- 点赞/收藏/关注、评论、通知中心
- DeepSeek AI 助手：流式对话（多轮记忆）+ 写作/润色/摘要/标签/代码/翻译/SEO/校对等 12 种模式
- 找回密码（重置令牌）、双 Token 刷新、管理后台（用户/文章/反馈管理）

## 管理员初始化

系统**无内置管理员密码**。两种方式设置首个管理员：

1. 方式一（推荐）：注册一个普通账号后，在数据库中手动执行：
   ```sql
   UPDATE user SET role='admin' WHERE username='你的用户名';
   ```
2. 方式二（仅限开发/内网）：设置环境变量 `ADMIN_BOOTSTRAP_ENABLED=true`，
   再注册 `ADMIN_BOOTSTRAP_USERNAME`（默认 `admin`）对应的账号（自行设置强密码），重启应用后自动提升为管理员。

登录后头部出现 🛡️ 管理入口，可管理用户/文章/反馈。

## 生产部署（Docker）

```bash
# 构建镜像
docker build -t blog-systme:latest .

# 运行（注意挂载 uploads 卷持久化上传文件）
docker run -d --name blog \
  -p 8080:8080 \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=你的数据库密码 \
  -e JWT_SECRET=你的JWT密钥 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://db-host:3306/blog_db?useSSL=true&serverTimezone=Asia/Shanghai \
  -e DEEPSEEK_API_KEY=sk-xxx \
  -e CORS_ORIGINS=https://你的前端域名 \
  -v blog-uploads:/app/uploads \
  blog-systme:latest
```

镜像内置：`SPRING_PROFILES_ACTIVE=prod`（自动关闭 Swagger）、`TZ=Asia/Shanghai`、健康检查、非 root 运行。

## 前端生产部署（Nginx）

```bash
# 前端仓库构建
cd blog-frontend && npm install && npm run build   # 产物在 dist/

# nginx.conf 关键配置
server {
    listen 80;
    server_name blog.example.com;
    root /usr/share/nginx/html;

    location /api/    { proxy_pass http://127.0.0.1:8080; proxy_set_header Host $host; }
    location /uploads/ { proxy_pass http://127.0.0.1:8080; }
    location /        { try_files $uri /index.html; }   # SPA 路由兜底
}
```

## 数据库备份

```bash
# 全量备份
mysqldump -u root -p blog_db > backup_$(date +%F).sql
# 上传文件备份
tar -czf uploads_$(date +%F).tar.gz uploads/
```

## 目录结构

```
src/main/resources/db/
├── schema.sql        # 完整初始化脚本（空库一键执行，幂等）
├── migration-v7.sql  # 存量库一次性迁移（外键/热榜/全文索引）
└── indexes.sql       # 存量老库的增量变更 + 孤儿数据清理脚本
```

## 安全特性

- JWT 双 Token + 改密失效（token_version）
- BCrypt 密码 + 复杂度校验 + 登录/注册限流
- 全链路 XSS 过滤（标题/内容/评论/昵称）+ DOMPurify 前端消毒
- 管理员 RBAC + 级联删除 + 最后管理员保护
- 手机/邮箱脱敏、安全响应头、CORS 白名单

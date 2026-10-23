# 安全体系（16 级纵深防御）

本文档定义 blog-system 的 16 级安全体系，由浅入深、一级比一级强，覆盖代码、运行时、部署与应急响应。每级标注实施状态：

- ✅ 已实现（代码/配置内置）
- 🟡 部署侧（需运维在部署时配置）
- 📋 流程侧（需团队约定与执行）

---

## L1 输入校验与参数净化 ✅

- 所有请求 DTO 使用 `jakarta.validation`（`@NotBlank` / `@Size` / `@Pattern` / `@Email`），`GlobalExceptionHandler` 统一返回 400。
- 昵称/标题/评论等可展示字段经 `XssUtil` 转义（服务端白名单清洗 + 前端 DOMPurify 消毒）。
- 密码复杂度统一策略：`PasswordUtil.PASSWORD_REGEX`（字母+数字+特殊字符，8-50 位）。
- 分页参数经 `PageUtil` 钳制，防超大 pageSize 拖垮查询。

## L2 SQL 注入防护（全参数化） ✅

- 全部 MyBatis SQL 使用 `#{}` 预编译参数，**无任何 `${}` 字符串拼接**。
- 排序/状态等动态字段不直接拼接 SQL；状态值走白名单（如反馈 `pending/resolved`）。
- 验证：`grep -rn '\${' src/main/resources/*.xml` 无结果。

## L3 密码哈希与复杂度 ✅

- BCrypt **cost=12**（企业级强度，`PasswordUtil`）；`matches()` 兼容旧 cost=10 密文。
- 注册/改密/重置全部走 `PasswordUtil.encode`，数据库不存明文。
- 密码 UTF-8 编码后限制在 **72 字节**内，避免 BCrypt 静默截断；正则同时限制 8–50 位。
- 找回密码重置令牌以 **SHA-256 哈希**入库，`token_hash` 唯一，单次有效并定时清理过期记录。

## L4 JWT 令牌强化 ✅

- HS256 签名密钥 **≥32 字节**（不足直接拒绝启动，防止弱密钥）。
- 校验 `issuer`（blog-system）与 `audience`（blog-web），防跨应用令牌混用。
- Access Token 有效期 **30 分钟**（缩短泄露窗口），Refresh Token 7 天。
- 两类令牌带 `typ=access|refresh` 声明；拦截器只接受 access，`/refresh` 只接受 refresh。
- 改密/重置/登出后 `token_version` 递增，旧 access 与 refresh **均立即失效**（每次请求校验 DB 版本）。

## L5 防暴力破解与令牌重放 ✅

- 登录限流：单 IP+账号 5 次/分钟 + 单 IP 30 次/分钟 + 单账号 10 次/分钟（`RateLimiterUtil`）。
- 登录前置图形验证码（4 位 PNG，单次有效）；**不再按账号硬锁定**，避免攻击者用错误密码冻结他人账号。
- **Refresh Token 旋转**：每次刷新递增 `token_version`，旧 refresh token 立即失效，防重放。

## L6 授权与越权防护（RBAC + IDOR） ✅

- 管理员接口逐一 `checkAdmin` 校验（403）；`setAdmin/revokeAdmin` 校验目标存在、不能撤销自己、保留最后一名管理员。
- 文章更新/删除、评论删除均校验属主；关注流强制登录（`AuthInterceptor`）。
- 管理端删除用户/文章执行级联清理（点赞/收藏/评论/通知/反馈/令牌），不留孤儿数据。

## L7 全局限流（DoW 防护） ✅

- `GlobalRateLimitFilter`：所有 `/api/**` 按 IP 600 次/分钟，超限返回 429 + `Retry-After`。
- 业务级细粒度限流叠加：注册、登录、找回密码、AI 调用。
- 信任模型：`server.forward-headers-strategy=none`；`ClientIpResolver` 仅在 `remoteAddr` 命中 `TRUSTED_PROXIES` CIDR 时采信转发头，并从右向左取第一个不可信地址。Nginx 必须用 `$remote_addr` 覆盖 `X-Forwarded-For`。

## L8 安全响应头 ✅

- `Content-Security-Policy` 覆盖 API、uploads 与非 Swagger 响应；`X-Frame-Options: DENY`、`X-Content-Type-Options: nosniff`。
- `Referrer-Policy`、`Permissions-Policy`、`Cross-Origin-Opener-Policy`、`Cross-Origin-Resource-Policy`、`X-Permitted-Cross-Domain-Policies`、`X-Download-Options`。
- **HSTS 始终下发**（浏览器只在 HTTPS 下生效），不依赖反向代理后的 `isSecure()`。
- 登录/刷新/找回/重置/验证码/导出等敏感响应统一 `Cache-Control: no-store`。

## L9 CORS 白名单严格化 ✅

- 使用精确 `allowedOrigins`（环境变量 `CORS_ORIGINS`，逗号分隔）；启动时检测到 `*`/通配符直接失败。
- `allowCredentials(false)`（JWT 走 `Authorization` 头，不需要跨域 cookie），不暴露 `Authorization` 响应头。
- 方法/请求头白名单（`Authorization`/`Content-Type`），生产环境 Swagger 关闭。

## L10 文件上传安全 ✅

- 仅 Base64 JSON 上传；大小上限 2MB（解码前后双重校验）。
- `ImageUtil` 魔数检测 + **解码前 ImageReader 读取宽高**（40MP 上限，防解压炸弹）+ 服务端重编码。
- 不接受 WebP（JDK 无内置解码器，无法安全重编码）；文件名随机化（UUID），无路径穿越风险。

## L11 敏感信息脱敏与日志安全 ✅

- 手机号/邮箱脱敏（`MaskUtil`），敏感日志中的用户名按需脱敏；管理端用户列表同时置空密码并脱敏邮箱/手机号。
- 日志**不打印密码/令牌**；审计日志写入与接口读取均对邮箱/手机号脱敏，保留 userId/username/IP 供安全排查。
- 账号注销在事务提交后删除当前及历史头像/封面文件（`uploads/avatar_<id>_*`、`cover_<id>_*`）。
- 文章/评论只允许本站 `/uploads/` 图片；封面 URL 白名单校验，Nginx CSP `img-src 'self' data:` 阻断外链追踪像素。
- 错误响应 `server.error.include-message/stacktrace=never`，不向客户端泄露堆栈。

## L12 依赖与供应链安全 🟡

- 依赖版本显式锁定（Spring Boot 4.1.0 / jjwt 0.12.6 / jsoup 1.18.1 / springdoc 2.8.12）。
- 建议：CI 集成 OWASP Dependency-Check 或启用 GitHub Dependabot + 安全告警；升级前查看 CVE 公告。

## L13 密钥与凭证管理 ✅/📋

- 数据库/JWT/DeepSeek 密钥全部环境变量注入，**代码与仓库无明文**（全历史已扫描验证）。
- 重置令牌哈希存储；`application-local.properties` 不入库不入镜像（`.dockerignore`）。
- 流程：JWT 密钥按季度轮换（轮换后需全员重新登录）；DeepSeek Key 按需轮换；DB 密码定期轮换。

## L14 安全审计日志 ✅

- 独立 `logs/audit.log`（20MB×14 天滚动，`AuditLogger`）。
- 记录：登录成功/失败、Refresh 拒绝、改密、密码重置申请/完成、管理员增删用户/文章、反馈处理、权限授予/撤销、管理员引导。

## L15 部署与运行环境加固 ✅/🟡

- Docker 非 root 运行（appuser）、多阶段构建、健康检查、优雅停机；`application-local.properties` 不入镜像。
- 生产建议：`docker run --cap-drop=ALL --read-only --security-opt no-new-privileges`；Nginx 侧强制 HTTPS/TLS1.2+；`/actuator` 仅内网可达。
- 数据库：compose 默认沿用 root；可选加固是 `ops/mysql-init` 创建 `blog_app`（仅 blog_db 范围 DDL/DML）与 `blog_backup`（SELECT/LOCK TABLES/SHOW VIEW/TRIGGER），启用后把 compose 的 `DB_USERNAME`/`DB_USER` 改为对应账号。备份使用 AES-256-CBC + PBKDF2 加密（`ops/backup.sh` / `ops/restore.sh`）。

## L16 纵深防御与应急响应 📋

- 前端 Nginx 层：WAF 规则（ModSecurity/Cloudflare）、IP 黑名单、`client_max_body_size 6m`。
- 监控告警：登录失败激增、429 突增、5xx 比例、CPU/内存（Prometheus/Grafana 或云监控）。
- 泄露应急：发现密钥/令牌泄露 → 立即轮换 `JWT_SECRET`、DB 密码、DeepSeek Key，审计 `logs/audit.log` 排查异常行为。
- 仓库侧：GitHub Secret Scanning（公开仓库自动启用）、提交前 `git grep -E "sk-|password|secret"` 自查、`.gitignore` 已覆盖日志/备份/上传/本地配置。

---

## 快速自检清单

```bash
# SQL 注入：应无输出
grep -rn '\${' src/main/resources --include='*.xml'

# 密钥泄露扫描：应无输出（不要在规则里写真实口令；排除本文档避免自命中）
git log --all -p -G '(AKIA|ASIA)[0-9A-Z]{16}|sk-[A-Za-z0-9_-]{16,}|(jwt[._-]?secret|password|secret)[[:space:]]*[=:][[:space:]]*[^$#{[:space:]]{8,}' \
  --format='COMMIT %h %ad %s' --date=short -- ':!SECURITY.md'

# 启动自检：无 JWT_SECRET 或弱密钥时应用拒绝启动
java -jar app.jar   # 期望输出 "JWT_SECRET 强度不足" 并退出
```

上线前必须完成：L12（Dependabot 开启）、L15 运维侧加固、L16 监控与应急流程。

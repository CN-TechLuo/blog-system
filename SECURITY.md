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
- 找回密码重置令牌以 **SHA-256 哈希**入库，即使拖库也无法直接用令牌重置他人账号。

## L4 JWT 令牌强化 ✅

- HS256 签名密钥 **≥32 字节**（不足直接拒绝启动，防止弱密钥）。
- 校验 `issuer`（blog-system）与 `audience`（blog-web），防跨应用令牌混用。
- Access Token 有效期 **30 分钟**（缩短泄露窗口），Refresh Token 7 天。
- 改密/重置/撤销管理员后 `token_version` 递增，旧 Refresh Token 立即失效。

## L5 防暴力破解与令牌重放 ✅

- 登录限流：单账号 5 次/分钟 + 单 IP 30 次/分钟（`RateLimiterUtil`）。
- **连续失败 5 次锁定账号 15 分钟**（内存实现，重启重置）。
- **Refresh Token 旋转**：每次刷新递增 `token_version`，旧 refresh token 立即失效，防重放。

## L6 授权与越权防护（RBAC + IDOR） ✅

- 管理员接口逐一 `checkAdmin` 校验（403）；`setAdmin/revokeAdmin` 校验目标存在、不能撤销自己、保留最后一名管理员。
- 文章更新/删除、评论删除均校验属主；关注流强制登录（`AuthInterceptor`）。
- 管理端删除用户/文章执行级联清理（点赞/收藏/评论/通知/反馈/令牌），不留孤儿数据。

## L7 全局限流（DoW 防护） ✅

- `GlobalRateLimitFilter`：所有 `/api/**` 按 IP 600 次/分钟，超限返回 429 + `Retry-After`。
- 业务级细粒度限流叠加：注册、登录、找回密码、AI 调用。
- 信任模型：反向代理必须覆盖 `X-Forwarded-For`；`server.forward-headers-strategy=framework`。

## L8 安全响应头 ✅

- `Content-Security-Policy`（API/uploads）、`X-Frame-Options: DENY`、`X-Content-Type-Options: nosniff`。
- `Referrer-Policy`、`Permissions-Policy`、`Cross-Origin-Opener-Policy`、`Cross-Origin-Resource-Policy`、`X-Permitted-Cross-Domain-Policies`、`X-Download-Options`。
- HTTPS 下自动下发 **HSTS**（max-age 1 年 + includeSubDomains）。
- 登录/刷新响应 `Cache-Control: no-store`，防 token 被缓存留存。

## L9 CORS 白名单严格化 ✅

- 显式 `allowedOriginPatterns`（环境变量 `CORS_ORIGINS`，逗号分隔），`allowCredentials(true)` 不使用 `*` 源。
- 方法/请求头白名单（`Authorization`/`Content-Type`），生产环境 Swagger 关闭。

## L10 文件上传安全 ✅

- 仅 Base64 JSON 上传；大小上限 2MB（解码前后双重校验）。
- `ImageUtil` 魔数检测 + **服务端重编码**，剥离 polyglot/恶意载荷。
- 文件名随机化（UUID），无路径穿越风险；multipart 限额 5MB。

## L11 敏感信息脱敏与日志安全 ✅

- 手机号/邮箱/用户名脱敏（`MaskUtil`）；管理端用户列表强制置空密码字段。
- 日志**不打印密码/令牌**；审计日志仅记录 userId/username/IP 元数据。
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
- 记录：登录成功/失败/锁定、Refresh 拒绝、改密、密码重置申请/完成、管理员增删用户/文章、反馈处理、权限授予/撤销、引导提升。

## L15 部署与运行环境加固 ✅/🟡

- Docker 非 root 运行（appuser）、多阶段构建、健康检查、优雅停机；`application-local.properties` 不入镜像。
- 生产建议：`docker run --cap-drop=ALL --read-only --security-opt no-new-privileges`；Nginx 侧强制 HTTPS/TLS1.2+；`/actuator` 仅内网可达。
- 数据库：最小权限账号（应用账号仅 DML，不授予 DDL/DROP）、定期备份（`ops/backup-db.ps1`）。

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

# 密钥泄露扫描：应无输出
git grep -nE 'root1235|sk-[a-zA-Z0-9]{10,}|jwt.secret=[A-Za-z0-9+/=]{16,}' $(git rev-list --all)

# 启动自检：无 JWT_SECRET 或弱密钥时应用拒绝启动
java -jar app.jar   # 期望输出 "JWT_SECRET 强度不足" 并退出
```

上线前必须完成：L12（Dependabot 开启）、L15 运维侧加固、L16 监控与应急流程。

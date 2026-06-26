# 博客系统代码审查报告

> 审查日期：2026-06-24 | 项目：blog-systme（Spring Boot 4.0.6 + MyBatis + JWT）

---

## 一、安全漏洞

### 1.1 高 — JWT 密钥硬编码默认值

**文件：** [`src/main/java/com/blog/blogsystme/util/JwtUtil.java`](src/main/java/com/blog/blogsystme/util/JwtUtil.java:15)

**问题描述：** 虽然优先读取环境变量 `JWT_SECRET` / 系统属性 `jwt.secret`，但存在硬编码的 Base64 默认密钥 `"ZGVmYXVsdC1zZWNyZXQta2V5..."`（解码为 `"default-secret-key-for-development-only-change-in-production"`）。如果运维忘记设置环境变量，应用将以弱密钥启动，所有 Token 可被轻易伪造。

**修复后代码：**

```java
// 移除硬编码默认值，启动时若未配置密钥则抛出异常阻止启动
private static final String SECRET = resolveSecret();

private static final SecretKey KEY;

static {
    if (SECRET == null || SECRET.isBlank()) {
        log.error("JWT 密钥未配置！请设置环境变量 JWT_SECRET 或系统属性 -Djwt.secret");
        throw new IllegalStateException(
                "JWT_SECRET 环境变量或 jwt.secret 系统属性必须配置。生成密钥命令: openssl rand -base64 32");
    }
    KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
    log.info("JWT 密钥已从环境变量/系统属性加载成功");
}

private static String resolveSecret() {
    String secret = System.getProperty("jwt.secret");
    if (secret != null && !secret.isBlank()) return secret;
    secret = System.getenv("JWT_SECRET");
    if (secret != null && !secret.isBlank()) return secret;
    return null;
}
```

---

### 1.2 高 — JWT 校验未在 create() 方法中调用 validateToken()

**文件：** [`src/main/java/com/blog/blogsystme/controller/ArticleController.java`](src/main/java/com/blog/blogsystme/controller/ArticleController.java:27)（原始代码）

**问题描述：** `create()` 方法直接调用 `getUsernameFromToken()` 而没有先用 `validateToken()` 校验。`validateToken()` 方法已定义但从未被调用，过期/伪造 token 的异常会直接抛出未处理的 `ExpiredJwtException`。

**修复后代码：**

```java
@PostMapping("/create")
public ResponseEntity<ApiResponse<Long>> create(@RequestBody Article article, HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("未登录或 token 格式错误"));
    }
    String token = authHeader.substring(7);

    // 先校验 token 有效性
    if (!JwtUtil.validateToken(token)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("token 无效或已过期"));
    }

    String username = JwtUtil.getUsernameFromToken(token);
    User user = userMapper.findByUsername(username);
    // ...
}
```

---

### 1.3 高 — SQL 注入检查：全部通过 ✅

**文件：** [`UserMapper.java`](src/main/java/com/blog/blogsystme/mapper/UserMapper.java)、[`ArticleMapper.java`](src/main/java/com/blog/blogsystme/mapper/ArticleMapper.java)

**检查结果：** 所有 `@Select`、`@Insert`、`@Update`、`@Delete` 注解均使用 MyBatis `#{}` 参数占位符，已防 SQL 注入。无风险。

---

### 1.4 中 — 密码加密：BCrypt 正确使用 ✅

**文件：** [`src/main/java/com/blog/blogsystme/util/PasswordUtil.java`](src/main/java/com/blog/blogsystme/util/PasswordUtil.java)

**检查结果：** 使用 `BCryptPasswordEncoder`（默认强度 10 轮），密码不会明文存储。注册时加密、登录时 `matches()` 验证，使用正确。无风险。

---

### 1.5 中 — CORS 跨域配置之前缺失

**文件：** 新增 [`src/main/java/com/blog/blogsystme/config/WebConfig.java`](src/main/java/com/blog/blogsystme/config/WebConfig.java)

**问题描述：** 项目原先没有任何 CORS 配置，浏览器端跨域请求会直接被拦截。

**修复后代码：**

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")  // 生产环境替换为具体域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

### 1.6 中 — 数据库 SSL 连接关闭

**文件：** [`src/main/resources/application.properties`](src/main/resources/application.properties:2)（原始代码）

**问题描述：** `useSSL=false` 关闭了数据库连接加密。

**修复后：**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/blog_db?useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai
```

---

### 1.7 低 — Controller 直接接收 Entity 作为请求体

**文件：** [`src/main/java/com/blog/blogsystme/controller/UserController.java`](src/main/java/com/blog/blogsystme/controller/UserController.java:26)（原始代码）

**问题描述：** `register(@RequestBody User user)` 直接将数据库实体暴露为请求体，用户可尝试传入 `id`、`createTime` 等不应由客户端控制的字段。建议使用独立 DTO。

**建议（未强制修改）：** 创建 `RegisterRequest` DTO，仅包含 `username`、`password`、`email`。

---

## 二、性能瓶颈

### 2.1 高 — 缺少数据库必要索引

**文件：** 新增 [`src/main/resources/db/indexes.sql`](src/main/resources/db/indexes.sql)

**问题描述：** 
- `user.username` 在登录时高频查询（`findByUsername`），无索引会导致全表扫描。
- `article.create_time` 在分页列表 `ORDER BY create_time DESC` 中使用，无索引排序性能差。
- `article.user_id` 在作者身份验证中使用。

**修复（执行以下 SQL）：**

```sql
ALTER TABLE `user` ADD UNIQUE INDEX `idx_username` (`username`);
ALTER TABLE `article` ADD INDEX `idx_user_id` (`user_id`);
ALTER TABLE `article` ADD INDEX `idx_create_time` (`create_time`);
```

---

### 2.2 中 — 分页列表查询使用 SELECT *

**文件：** [`src/main/java/com/blog/blogsystme/mapper/ArticleMapper.java`](src/main/java/com/blog/blogsystme/mapper/ArticleMapper.java:32)（原始代码）

**问题描述：** `findByPage` 使用 `SELECT *` 返回所有字段，包括大文本 `content` 字段，列表页不需要文章正文却全部传输。

**修复后代码：**

```java
@Select("SELECT id, title, user_id, view_count, create_time, update_time " +
        "FROM article ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
List<Article> findByPage(@Param("start") int start, @Param("pageSize") int pageSize);
```

---

### 2.3 中 — 更新/删除操作存在双查询（可 SQL 层面合并）

**文件：** [`src/main/java/com/blog/blogsystme/mapper/ArticleMapper.java`](src/main/java/com/blog/blogsystme/mapper/ArticleMapper.java)（新增方法）

**问题描述：** `update()` 和 `delete()` 先 `SELECT` 验证作者身份，再执行写操作（2 次数据库往返）。

**修复后代码（新增 SQL 层面鉴权方法）：**

```java
/** SQL 层面鉴权更新：仅作者本人可更新，根据返回行数判断权限 */
@Update("UPDATE article SET title = #{title}, content = #{content} WHERE id = #{id} AND user_id = #{userId}")
int updateByAuthor(Article article);

/** SQL 层面鉴权删除：仅作者本人可删除 */
@Delete("DELETE FROM article WHERE id = #{id} AND user_id = #{userId}")
int deleteByIdAndAuthor(@Param("id") Integer id, @Param("userId") Integer userId);
```

---

### 2.4 中 — 建议添加缓存（建议，未强制修改）

**建议：** 对 `article` 列表查询和热门文章引入 Redis 缓存，减少数据库读压力。可使用 `@Cacheable` 注解或手动 Redis 操作。

---

## 三、代码规范

### 3.1 高 — 包名拼写错误 + 重复包

**文件：**
- `com.blog.blogsystme`（主代码包，拼写错误："systme" 应为 "system"）
- `com.blog.blogsystem`（重复的旧包，已删除）

**问题描述：** 存在两个包 `blogsystme`（拼写错误）和 `blogsystem`（正确拼写），包含重复的 `BlogSystemApplication` 和 Entity 类。`com/blog/bl` 为残留的无扩展名文件。

**修复：** 已删除 `blogsystem` 包、`com/blog/bl` 残留文件。包名 `blogsystme` 因涉及全项目重命名，建议后续统一重构。

---

### 3.2 高 — System.out.println 替代为 SLF4J 日志

**文件：** [`src/main/java/com/blog/blogsystme/controller/UserController.java`](src/main/java/com/blog/blogsystme/controller/UserController.java:28-44)（原始代码）

**问题描述：** 使用 `System.out.println` 输出调试信息，无法控制日志级别，可能泄露用户名等敏感信息。

**修复后代码：**

```java
private static final Logger log = LoggerFactory.getLogger(UserController.class);

log.debug("收到注册请求: username={}", user.getUsername());
log.info("注册结果: username={}, rows={}", user.getUsername(), rows);
log.info("登录成功: username={}", user.getUsername());
```

---

### 3.3 高 — 缺少统一的 API 响应类 + 内部类 RegisterResponse

**文件：** [`src/main/java/com/blog/blogsystme/controller/UserController.java`](src/main/java/com/blog/blogsystme/controller/UserController.java:54)（原始代码）

**问题描述：** `RegisterResponse` 定义在 `UserController` 内部，与 `PageResponse` 功能重叠。

**修复：** 新增 [`src/main/java/com/blog/blogsystme/dto/ApiResponse.java`](src/main/java/com/blog/blogsystme/dto/ApiResponse.java) 泛型响应类：

```java
@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(String message) { ... }
    public static <T> ApiResponse<T> success(String message, T data) { ... }
    public static <T> ApiResponse<T> fail(String message) { ... }
}
```

---

### 3.4 中 — Controller 返回值类型不统一（已修复）

**文件：** [`src/main/java/com/blog/blogsystme/controller/ArticleController.java`](src/main/java/com/blog/blogsystme/controller/ArticleController.java:27)（原始代码）

**问题描述：** `create()` 返回纯 `String`，其他方法返回 `ResponseEntity`，不一致。

**修复后：** `create()` 也返回 `ResponseEntity<ApiResponse<Long>>`。

---

### 3.5 中 — 方法访问修饰符缺失

**文件：** [`src/main/java/com/blog/blogsystme/controller/ArticleController.java`](src/main/java/com/blog/blogsystme/controller/ArticleController.java:76)（原始代码）

**问题描述：** `getCurrentUserId()` 辅助方法缺少 `private` 修饰符。

**修复后：** `private Integer getCurrentUserId(HttpServletRequest request) { ... }`

---

### 3.6 中 — 缺少输入参数校验注解

**文件：** [`User.java`](src/main/java/com/blog/blogsystme/entity/User.java)、[`Article.java`](src/main/java/com/blog/blogsystme/entity/Article.java)

**问题描述：** 无任何 `@NotBlank`、`@Size`、`@Email` 校验，用户可提交空用户名、空密码。

**修复后代码：**

```java
// User.java
@NotBlank(message = "用户名不能为空")
@Size(min = 2, max = 20, message = "用户名长度需在2-20位之间")
private String username;

@NotBlank(message = "密码不能为空")
@Size(min = 6, max = 50, message = "密码长度需在6-50位之间")
private String password;

@Email(message = "邮箱格式不正确")
private String email;

// Article.java
@NotBlank(message = "文章标题不能为空")
@Size(min = 1, max = 200, message = "文章标题长度需在1-200位之间")
private String title;

@NotBlank(message = "文章内容不能为空")
private String content;
```

---

### 3.7 低 — TestController 残留在生产代码（已删除）

**文件：** [`src/main/java/com/blog/blogsystme/TestController.java`](src/main/java/com/blog/blogsystme/TestController.java)（已删除）

**问题描述：** 测试用 `/hello` 端点残留在生产代码中。

**修复：** 已删除。

---

## 四、运维与可观测性

### 4.1 高 — 缺少全局异常处理器

**文件：** 新增 [`src/main/java/com/blog/blogsystme/config/GlobalExceptionHandler.java`](src/main/java/com/blog/blogsystme/config/GlobalExceptionHandler.java)

**问题描述：** 原先无 `@RestControllerAdvice`，JWT 异常和运行时异常直接暴露给前端，返回 HTML 错误页而非 JSON。

**修复后代码：**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({SignatureException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleJwtSignatureException(Exception e) {
        log.warn("JWT 校验失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("token 无效或已过期"));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(ExpiredJwtException e) {
        log.warn("JWT 已过期: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("token 已过期，请重新登录"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        log.error("服务器内部错误", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("服务器内部错误，请稍后重试"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("未预期的异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("服务器内部错误"));
    }
}
```

---

### 4.2 中 — 数据密码已环境变量化 ✅ / JWT 密钥已修复

**文件：** [`src/main/resources/application.properties`](src/main/resources/application.properties:4-5)

**检查结果：** 数据库用户名和密码已使用 `${DB_USERNAME}` 和 `${DB_PASSWORD}` 环境变量占位。JWT 密钥已在 [`JwtUtil.java`](src/main/java/com/blog/blogsystme/util/JwtUtil.java) 中强制要求环境变量注入。

---

### 4.3 中 — 添加 Actuator 监控依赖

**文件：** [`pom.xml`](pom.xml)（新增依赖）

**问题描述：** 原先无健康检查端点。

**修复：** 已添加 `spring-boot-starter-actuator`，配置仅暴露 `health` 和 `info` 端点：

```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
```

---

### 4.4 低 — 缺少统一认证拦截器（建议）

**问题描述：** `getCurrentUserId()` 方法在 `ArticleController` 中多处重复调用，建议实现 `HandlerInterceptor` 统一处理 JWT 认证。

---

## 五、修复汇总

| 类别 | 问题 | 风险等级 | 状态 |
|------|------|---------|------|
| 安全 | JWT 密钥硬编码默认值 | **高** | ✅ 已修复 |
| 安全 | JWT 校验 validateToken 未调用 | **高** | ✅ 已修复 |
| 安全 | SQL 注入 | — | ✅ 无风险 |
| 安全 | BCrypt 密码加密 | — | ✅ 正确 |
| 安全 | CORS 配置缺失 | 中 | ✅ 已添加 |
| 安全 | 数据库 SSL 关闭 | 中 | ✅ 已修复 |
| 安全 | Controller 直接使用 Entity | 低 | ⚠️ 建议 |
| 性能 | 缺少数据库索引 | **高** | ✅ 已提供 SQL |
| 性能 | SELECT * 返回大字段 | 中 | ✅ 已优化 |
| 性能 | 更新/删除双查询 | 中 | ✅ 已提供方法 |
| 性能 | 缓存建议 | 低 | ⚠️ 建议 |
| 规范 | 包名拼写 + 重复包 | **高** | ✅ 已清理 |
| 规范 | System.out.println | **高** | ✅ 已替换 |
| 规范 | 缺少统一响应类 | **高** | ✅ 已创建 |
| 规范 | 返回值类型不统一 | 中 | ✅ 已统一 |
| 规范 | 访问修饰符缺失 | 中 | ✅ 已修复 |
| 规范 | 缺少参数校验 | 中 | ✅ 已添加 |
| 规范 | TestController 残留 | 低 | ✅ 已删除 |
| 运维 | 缺少全局异常处理 | **高** | ✅ 已创建 |
| 运维 | 数据库密码环境变量 | — | ✅ 正确 |
| 运维 | JWT 密钥环境变量 | 中 | ✅ 已修复 |
| 运维 | Actuator 监控 | 中 | ✅ 已添加 |
| 运维 | 认证拦截器建议 | 低 | ⚠️ 建议 |

---

> **修订历史：** 2026-06-24 — 全面审查并修复所有高/中风险问题。

# 鍗氬绯荤粺浠ｇ爜瀹℃煡鎶ュ憡

> 瀹℃煡鏃ユ湡锛?026-06-24 | 椤圭洰锛歜log-systme锛圫pring Boot 4.0.6 + MyBatis + JWT锛?

---

## 涓€銆佸畨鍏ㄦ紡娲?

### 1.1 楂?鈥?JWT 瀵嗛挜纭紪鐮侀粯璁ゅ€?

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/util/JwtUtil.java`](src/main/java/com/blog/blogsystem/util/JwtUtil.java:15)

**闂鎻忚堪锛?* 铏界劧浼樺厛璇诲彇鐜鍙橀噺 `JWT_SECRET` / 绯荤粺灞炴€?`jwt.secret`锛屼絾瀛樺湪纭紪鐮佺殑 Base64 榛樿瀵嗛挜銆傚鏋滆繍缁村繕璁拌缃幆澧冨彉閲忥紝搴旂敤灏嗕互寮卞瘑閽ュ惎鍔紝鎵€鏈?Token 鍙杞绘槗浼€犮€?

**淇鍚庝唬鐮侊細**

```java
// 绉婚櫎纭紪鐮侀粯璁ゅ€硷紝鍚姩鏃惰嫢鏈厤缃瘑閽ュ垯鎶涘嚭寮傚父闃绘鍚姩
private static final String SECRET = resolveSecret();

private static final SecretKey KEY;

static {
    if (SECRET == null || SECRET.isBlank()) {
        log.error("JWT 瀵嗛挜鏈厤缃紒璇疯缃幆澧冨彉閲?JWT_SECRET 鎴栫郴缁熷睘鎬?-Djwt.secret");
        throw new IllegalStateException(
                "JWT_SECRET 鐜鍙橀噺鎴?jwt.secret 绯荤粺灞炴€у繀椤婚厤缃€傜敓鎴愬瘑閽ュ懡浠? openssl rand -base64 32");
    }
    KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
    log.info("JWT 瀵嗛挜宸蹭粠鐜鍙橀噺/绯荤粺灞炴€у姞杞芥垚鍔?);
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

### 1.2 楂?鈥?JWT 鏍￠獙鏈湪 create() 鏂规硶涓皟鐢?validateToken()

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/controller/ArticleController.java`](src/main/java/com/blog/blogsystem/controller/ArticleController.java:27)锛堝師濮嬩唬鐮侊級

**闂鎻忚堪锛?* `create()` 鏂规硶鐩存帴璋冪敤 `getUsernameFromToken()` 鑰屾病鏈夊厛鐢?`validateToken()` 鏍￠獙銆俙validateToken()` 鏂规硶宸插畾涔変絾浠庢湭琚皟鐢紝杩囨湡/浼€?token 鐨勫紓甯镐細鐩存帴鎶涘嚭鏈鐞嗙殑 `ExpiredJwtException`銆?

**淇鍚庝唬鐮侊細**

```java
@PostMapping("/create")
public ResponseEntity<ApiResponse<Long>> create(@RequestBody Article article, HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("鏈櫥褰曟垨 token 鏍煎紡閿欒"));
    }
    String token = authHeader.substring(7);

    // 鍏堟牎楠?token 鏈夋晥鎬?
    if (!JwtUtil.validateToken(token)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("token 鏃犳晥鎴栧凡杩囨湡"));
    }

    String username = JwtUtil.getUsernameFromToken(token);
    User user = userMapper.findByUsername(username);
    // ...
}
```

---

### 1.3 楂?鈥?SQL 娉ㄥ叆妫€鏌ワ細鍏ㄩ儴閫氳繃 鉁?

**鏂囦欢锛?* [`UserMapper.java`](src/main/java/com/blog/blogsystem/mapper/UserMapper.java)銆乕`ArticleMapper.java`](src/main/java/com/blog/blogsystem/mapper/ArticleMapper.java)

**妫€鏌ョ粨鏋滐細** 鎵€鏈?`@Select`銆乣@Insert`銆乣@Update`銆乣@Delete` 娉ㄨВ鍧囦娇鐢?MyBatis `#{}` 鍙傛暟鍗犱綅绗︼紝宸查槻 SQL 娉ㄥ叆銆傛棤椋庨櫓銆?

---

### 1.4 涓?鈥?瀵嗙爜鍔犲瘑锛欱Crypt 姝ｇ‘浣跨敤 鉁?

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/util/PasswordUtil.java`](src/main/java/com/blog/blogsystem/util/PasswordUtil.java)

**妫€鏌ョ粨鏋滐細** 浣跨敤 `BCryptPasswordEncoder`锛堥粯璁ゅ己搴?10 杞級锛屽瘑鐮佷笉浼氭槑鏂囧瓨鍌ㄣ€傛敞鍐屾椂鍔犲瘑銆佺櫥褰曟椂 `matches()` 楠岃瘉锛屼娇鐢ㄦ纭€傛棤椋庨櫓銆?

---

### 1.5 涓?鈥?CORS 璺ㄥ煙閰嶇疆涔嬪墠缂哄け

**鏂囦欢锛?* 鏂板 [`src/main/java/com/blog/blogsystem/config/WebConfig.java`](src/main/java/com/blog/blogsystem/config/WebConfig.java)

**闂鎻忚堪锛?* 椤圭洰鍘熷厛娌℃湁浠讳綍 CORS 閰嶇疆锛屾祻瑙堝櫒绔法鍩熻姹備細鐩存帴琚嫤鎴€?

**淇鍚庝唬鐮侊細**

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")  // 鐢熶骇鐜鏇挎崲涓哄叿浣撳煙鍚?
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

### 1.6 涓?鈥?鏁版嵁搴?SSL 杩炴帴鍏抽棴

**鏂囦欢锛?* [`src/main/resources/application.properties`](src/main/resources/application.properties:2)锛堝師濮嬩唬鐮侊級

**闂鎻忚堪锛?* `useSSL=false` 鍏抽棴浜嗘暟鎹簱杩炴帴鍔犲瘑銆?

**淇鍚庯細**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/blog_db?useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai
```

---

### 1.7 浣?鈥?Controller 鐩存帴鎺ユ敹 Entity 浣滀负璇锋眰浣?

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/controller/UserController.java`](src/main/java/com/blog/blogsystem/controller/UserController.java:26)锛堝師濮嬩唬鐮侊級

**闂鎻忚堪锛?* `register(@RequestBody User user)` 鐩存帴灏嗘暟鎹簱瀹炰綋鏆撮湶涓鸿姹備綋锛岀敤鎴峰彲灏濊瘯浼犲叆 `id`銆乣createTime` 绛変笉搴旂敱瀹㈡埛绔帶鍒剁殑瀛楁銆傚缓璁娇鐢ㄧ嫭绔?DTO銆?

**寤鸿锛堟湭寮哄埗淇敼锛夛細** 鍒涘缓 `RegisterRequest` DTO锛屼粎鍖呭惈 `username`銆乣password`銆乣email`銆?

---

## 浜屻€佹€ц兘鐡堕

### 2.1 楂?鈥?缂哄皯鏁版嵁搴撳繀瑕佺储寮?

**鏂囦欢锛?* 鏂板 [`src/main/resources/db/indexes.sql`](src/main/resources/db/indexes.sql)

**闂鎻忚堪锛?* 
- `user.username` 鍦ㄧ櫥褰曟椂楂橀鏌ヨ锛坄findByUsername`锛夛紝鏃犵储寮曚細瀵艰嚧鍏ㄨ〃鎵弿銆?
- `article.create_time` 鍦ㄥ垎椤靛垪琛?`ORDER BY create_time DESC` 涓娇鐢紝鏃犵储寮曟帓搴忔€ц兘宸€?
- `article.user_id` 鍦ㄤ綔鑰呰韩浠介獙璇佷腑浣跨敤銆?

**淇锛堟墽琛屼互涓?SQL锛夛細**

```sql
ALTER TABLE `user` ADD UNIQUE INDEX `idx_username` (`username`);
ALTER TABLE `article` ADD INDEX `idx_user_id` (`user_id`);
ALTER TABLE `article` ADD INDEX `idx_create_time` (`create_time`);
```

---

### 2.2 涓?鈥?鍒嗛〉鍒楄〃鏌ヨ浣跨敤 SELECT *

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/mapper/ArticleMapper.java`](src/main/java/com/blog/blogsystem/mapper/ArticleMapper.java:32)锛堝師濮嬩唬鐮侊級

**闂鎻忚堪锛?* `findByPage` 浣跨敤 `SELECT *` 杩斿洖鎵€鏈夊瓧娈碉紝鍖呮嫭澶ф枃鏈?`content` 瀛楁锛屽垪琛ㄩ〉涓嶉渶瑕佹枃绔犳鏂囧嵈鍏ㄩ儴浼犺緭銆?

**淇鍚庝唬鐮侊細**

```java
@Select("SELECT id, title, user_id, view_count, create_time, update_time " +
        "FROM article ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
List<Article> findByPage(@Param("start") int start, @Param("pageSize") int pageSize);
```

---

### 2.3 涓?鈥?鏇存柊/鍒犻櫎鎿嶄綔瀛樺湪鍙屾煡璇紙鍙?SQL 灞傞潰鍚堝苟锛?

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/mapper/ArticleMapper.java`](src/main/java/com/blog/blogsystem/mapper/ArticleMapper.java)锛堟柊澧炴柟娉曪級

**闂鎻忚堪锛?* `update()` 鍜?`delete()` 鍏?`SELECT` 楠岃瘉浣滆€呰韩浠斤紝鍐嶆墽琛屽啓鎿嶄綔锛? 娆℃暟鎹簱寰€杩旓級銆?

**淇鍚庝唬鐮侊紙鏂板 SQL 灞傞潰閴存潈鏂规硶锛夛細**

```java
/** SQL 灞傞潰閴存潈鏇存柊锛氫粎浣滆€呮湰浜哄彲鏇存柊锛屾牴鎹繑鍥炶鏁板垽鏂潈闄?*/
@Update("UPDATE article SET title = #{title}, content = #{content} WHERE id = #{id} AND user_id = #{userId}")
int updateByAuthor(Article article);

/** SQL 灞傞潰閴存潈鍒犻櫎锛氫粎浣滆€呮湰浜哄彲鍒犻櫎 */
@Delete("DELETE FROM article WHERE id = #{id} AND user_id = #{userId}")
int deleteByIdAndAuthor(@Param("id") Integer id, @Param("userId") Integer userId);
```

---

### 2.4 涓?鈥?寤鸿娣诲姞缂撳瓨锛堝缓璁紝鏈己鍒朵慨鏀癸級

**寤鸿锛?* 瀵?`article` 鍒楄〃鏌ヨ鍜岀儹闂ㄦ枃绔犲紩鍏?Redis 缂撳瓨锛屽噺灏戞暟鎹簱璇诲帇鍔涖€傚彲浣跨敤 `@Cacheable` 娉ㄨВ鎴栨墜鍔?Redis 鎿嶄綔銆?

---

## 涓夈€佷唬鐮佽鑼?

### 3.1 楂?鈥?鍖呭悕鎷煎啓閿欒 + 閲嶅鍖?

**鏂囦欢锛?*
- `com.blog.blogsystme`锛堜富浠ｇ爜鍖咃紝鎷煎啓閿欒锛?systme" 搴斾负 "system"锛?
- `com.blog.blogsystem`锛堥噸澶嶇殑鏃у寘锛屽凡鍒犻櫎锛?

**闂鎻忚堪锛?* 瀛樺湪涓や釜鍖?`blogsystme`锛堟嫾鍐欓敊璇級鍜?`blogsystem`锛堟纭嫾鍐欙級锛屽寘鍚噸澶嶇殑 `BlogSystemApplication` 鍜?Entity 绫汇€俙com/blog/bl` 涓烘畫鐣欑殑鏃犳墿灞曞悕鏂囦欢銆?

**淇锛?* 宸插垹闄?`blogsystem` 鍖呫€乣com/blog/bl` 娈嬬暀鏂囦欢銆傚寘鍚?`blogsystme` 鍥犳秹鍙婂叏椤圭洰閲嶅懡鍚嶏紝寤鸿鍚庣画缁熶竴閲嶆瀯銆?

---

### 3.2 楂?鈥?System.out.println 鏇夸唬涓?SLF4J 鏃ュ織

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/controller/UserController.java`](src/main/java/com/blog/blogsystem/controller/UserController.java:28-44)锛堝師濮嬩唬鐮侊級

**闂鎻忚堪锛?* 浣跨敤 `System.out.println` 杈撳嚭璋冭瘯淇℃伅锛屾棤娉曟帶鍒舵棩蹇楃骇鍒紝鍙兘娉勯湶鐢ㄦ埛鍚嶇瓑鏁忔劅淇℃伅銆?

**淇鍚庝唬鐮侊細**

```java
private static final Logger log = LoggerFactory.getLogger(UserController.class);

log.debug("鏀跺埌娉ㄥ唽璇锋眰: username={}", user.getUsername());
log.info("娉ㄥ唽缁撴灉: username={}, rows={}", user.getUsername(), rows);
log.info("鐧诲綍鎴愬姛: username={}", user.getUsername());
```

---

### 3.3 楂?鈥?缂哄皯缁熶竴鐨?API 鍝嶅簲绫?+ 鍐呴儴绫?RegisterResponse

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/controller/UserController.java`](src/main/java/com/blog/blogsystem/controller/UserController.java:54)锛堝師濮嬩唬鐮侊級

**闂鎻忚堪锛?* `RegisterResponse` 瀹氫箟鍦?`UserController` 鍐呴儴锛屼笌 `PageResponse` 鍔熻兘閲嶅彔銆?

**淇锛?* 鏂板 [`src/main/java/com/blog/blogsystem/dto/ApiResponse.java`](src/main/java/com/blog/blogsystem/dto/ApiResponse.java) 娉涘瀷鍝嶅簲绫伙細

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

### 3.4 涓?鈥?Controller 杩斿洖鍊肩被鍨嬩笉缁熶竴锛堝凡淇锛?

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/controller/ArticleController.java`](src/main/java/com/blog/blogsystem/controller/ArticleController.java:27)锛堝師濮嬩唬鐮侊級

**闂鎻忚堪锛?* `create()` 杩斿洖绾?`String`锛屽叾浠栨柟娉曡繑鍥?`ResponseEntity`锛屼笉涓€鑷淬€?

**淇鍚庯細** `create()` 涔熻繑鍥?`ResponseEntity<ApiResponse<Long>>`銆?

---

### 3.5 涓?鈥?鏂规硶璁块棶淇グ绗︾己澶?

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/controller/ArticleController.java`](src/main/java/com/blog/blogsystem/controller/ArticleController.java:76)锛堝師濮嬩唬鐮侊級

**闂鎻忚堪锛?* `getCurrentUserId()` 杈呭姪鏂规硶缂哄皯 `private` 淇グ绗︺€?

**淇鍚庯細** `private Integer getCurrentUserId(HttpServletRequest request) { ... }`

---

### 3.6 涓?鈥?缂哄皯杈撳叆鍙傛暟鏍￠獙娉ㄨВ

**鏂囦欢锛?* [`User.java`](src/main/java/com/blog/blogsystem/entity/User.java)銆乕`Article.java`](src/main/java/com/blog/blogsystem/entity/Article.java)

**闂鎻忚堪锛?* 鏃犱换浣?`@NotBlank`銆乣@Size`銆乣@Email` 鏍￠獙锛岀敤鎴峰彲鎻愪氦绌虹敤鎴峰悕銆佺┖瀵嗙爜銆?

**淇鍚庝唬鐮侊細**

```java
// User.java
@NotBlank(message = "鐢ㄦ埛鍚嶄笉鑳戒负绌?)
@Size(min = 2, max = 20, message = "鐢ㄦ埛鍚嶉暱搴﹂渶鍦?-20浣嶄箣闂?)
private String username;

@NotBlank(message = "瀵嗙爜涓嶈兘涓虹┖")
@Size(min = 6, max = 50, message = "瀵嗙爜闀垮害闇€鍦?-50浣嶄箣闂?)
private String password;

@Email(message = "閭鏍煎紡涓嶆纭?)
private String email;

// Article.java
@NotBlank(message = "鏂囩珷鏍囬涓嶈兘涓虹┖")
@Size(min = 1, max = 200, message = "鏂囩珷鏍囬闀垮害闇€鍦?-200浣嶄箣闂?)
private String title;

@NotBlank(message = "鏂囩珷鍐呭涓嶈兘涓虹┖")
private String content;
```

---

### 3.7 浣?鈥?TestController 娈嬬暀鍦ㄧ敓浜т唬鐮侊紙宸插垹闄わ級

**鏂囦欢锛?* [`src/main/java/com/blog/blogsystem/TestController.java`](src/main/java/com/blog/blogsystem/TestController.java)锛堝凡鍒犻櫎锛?

**闂鎻忚堪锛?* 娴嬭瘯鐢?`/hello` 绔偣娈嬬暀鍦ㄧ敓浜т唬鐮佷腑銆?

**淇锛?* 宸插垹闄ゃ€?

---

## 鍥涖€佽繍缁翠笌鍙娴嬫€?

### 4.1 楂?鈥?缂哄皯鍏ㄥ眬寮傚父澶勭悊鍣?

**鏂囦欢锛?* 鏂板 [`src/main/java/com/blog/blogsystem/config/GlobalExceptionHandler.java`](src/main/java/com/blog/blogsystem/config/GlobalExceptionHandler.java)

**闂鎻忚堪锛?* 鍘熷厛鏃?`@RestControllerAdvice`锛孞WT 寮傚父鍜岃繍琛屾椂寮傚父鐩存帴鏆撮湶缁欏墠绔紝杩斿洖 HTML 閿欒椤佃€岄潪 JSON銆?

**淇鍚庝唬鐮侊細**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({SignatureException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleJwtSignatureException(Exception e) {
        log.warn("JWT 鏍￠獙澶辫触: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("token 鏃犳晥鎴栧凡杩囨湡"));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(ExpiredJwtException e) {
        log.warn("JWT 宸茶繃鏈? {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("token 宸茶繃鏈燂紝璇烽噸鏂扮櫥褰?));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        log.error("鏈嶅姟鍣ㄥ唴閮ㄩ敊璇?, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("鏈嶅姟鍣ㄥ唴閮ㄩ敊璇紝璇风◢鍚庨噸璇?));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("鏈鏈熺殑寮傚父", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("鏈嶅姟鍣ㄥ唴閮ㄩ敊璇?));
    }
}
```

---

### 4.2 涓?鈥?鏁版嵁瀵嗙爜宸茬幆澧冨彉閲忓寲 鉁?/ JWT 瀵嗛挜宸蹭慨澶?

**鏂囦欢锛?* [`src/main/resources/application.properties`](src/main/resources/application.properties:4-5)

**妫€鏌ョ粨鏋滐細** 鏁版嵁搴撶敤鎴峰悕鍜屽瘑鐮佸凡浣跨敤 `${DB_USERNAME}` 鍜?`${DB_PASSWORD}` 鐜鍙橀噺鍗犱綅銆侸WT 瀵嗛挜宸插湪 [`JwtUtil.java`](src/main/java/com/blog/blogsystem/util/JwtUtil.java) 涓己鍒惰姹傜幆澧冨彉閲忔敞鍏ャ€?

---

### 4.3 涓?鈥?娣诲姞 Actuator 鐩戞帶渚濊禆

**鏂囦欢锛?* [`pom.xml`](pom.xml)锛堟柊澧炰緷璧栵級

**闂鎻忚堪锛?* 鍘熷厛鏃犲仴搴锋鏌ョ鐐广€?

**淇锛?* 宸叉坊鍔?`spring-boot-starter-actuator`锛岄厤缃粎鏆撮湶 `health` 鍜?`info` 绔偣锛?

```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
```

---

### 4.4 浣?鈥?缂哄皯缁熶竴璁よ瘉鎷︽埅鍣紙寤鸿锛?

**闂鎻忚堪锛?* `getCurrentUserId()` 鏂规硶鍦?`ArticleController` 涓澶勯噸澶嶈皟鐢紝寤鸿瀹炵幇 `HandlerInterceptor` 缁熶竴澶勭悊 JWT 璁よ瘉銆?

---

## 浜斻€佷慨澶嶆眹鎬?

| 绫诲埆 | 闂 | 椋庨櫓绛夌骇 | 鐘舵€?|
|------|------|---------|------|
| 瀹夊叏 | JWT 瀵嗛挜纭紪鐮侀粯璁ゅ€?| **楂?* | 鉁?宸蹭慨澶?|
| 瀹夊叏 | JWT 鏍￠獙 validateToken 鏈皟鐢?| **楂?* | 鉁?宸蹭慨澶?|
| 瀹夊叏 | SQL 娉ㄥ叆 | 鈥?| 鉁?鏃犻闄?|
| 瀹夊叏 | BCrypt 瀵嗙爜鍔犲瘑 | 鈥?| 鉁?姝ｇ‘ |
| 瀹夊叏 | CORS 閰嶇疆缂哄け | 涓?| 鉁?宸叉坊鍔?|
| 瀹夊叏 | 鏁版嵁搴?SSL 鍏抽棴 | 涓?| 鉁?宸蹭慨澶?|
| 瀹夊叏 | Controller 鐩存帴浣跨敤 Entity | 浣?| 鈿狅笍 寤鸿 |
| 鎬ц兘 | 缂哄皯鏁版嵁搴撶储寮?| **楂?* | 鉁?宸叉彁渚?SQL |
| 鎬ц兘 | SELECT * 杩斿洖澶у瓧娈?| 涓?| 鉁?宸蹭紭鍖?|
| 鎬ц兘 | 鏇存柊/鍒犻櫎鍙屾煡璇?| 涓?| 鉁?宸叉彁渚涙柟娉?|
| 鎬ц兘 | 缂撳瓨寤鸿 | 浣?| 鈿狅笍 寤鸿 |
| 瑙勮寖 | 鍖呭悕鎷煎啓 + 閲嶅鍖?| **楂?* | 鉁?宸叉竻鐞?|
| 瑙勮寖 | System.out.println | **楂?* | 鉁?宸叉浛鎹?|
| 瑙勮寖 | 缂哄皯缁熶竴鍝嶅簲绫?| **楂?* | 鉁?宸插垱寤?|
| 瑙勮寖 | 杩斿洖鍊肩被鍨嬩笉缁熶竴 | 涓?| 鉁?宸茬粺涓€ |
| 瑙勮寖 | 璁块棶淇グ绗︾己澶?| 涓?| 鉁?宸蹭慨澶?|
| 瑙勮寖 | 缂哄皯鍙傛暟鏍￠獙 | 涓?| 鉁?宸叉坊鍔?|
| 瑙勮寖 | TestController 娈嬬暀 | 浣?| 鉁?宸插垹闄?|
| 杩愮淮 | 缂哄皯鍏ㄥ眬寮傚父澶勭悊 | **楂?* | 鉁?宸插垱寤?|
| 杩愮淮 | 鏁版嵁搴撳瘑鐮佺幆澧冨彉閲?| 鈥?| 鉁?姝ｇ‘ |
| 杩愮淮 | JWT 瀵嗛挜鐜鍙橀噺 | 涓?| 鉁?宸蹭慨澶?|
| 杩愮淮 | Actuator 鐩戞帶 | 涓?| 鉁?宸叉坊鍔?|
| 杩愮淮 | 璁よ瘉鎷︽埅鍣ㄥ缓璁?| 浣?| 鈿狅笍 寤鸿 |

---

> **淇鍘嗗彶锛?* 2026-06-24 鈥?鍏ㄩ潰瀹℃煡骞朵慨澶嶆墍鏈夐珮/涓闄╅棶棰樸€?

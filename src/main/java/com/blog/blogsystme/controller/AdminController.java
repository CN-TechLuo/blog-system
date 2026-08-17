package com.blog.blogsystme.controller;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.ArticleMapper;
import com.blog.blogsystme.mapper.BookmarkMapper;
import com.blog.blogsystme.mapper.CommentMapper;
import com.blog.blogsystme.mapper.FeedbackMapper;
import com.blog.blogsystme.mapper.FollowMapper;
import com.blog.blogsystme.mapper.LikeMapper;
import com.blog.blogsystme.mapper.NotificationMapper;
import com.blog.blogsystme.mapper.PasswordResetTokenMapper;
import com.blog.blogsystme.mapper.UserMapper;
import com.blog.blogsystme.util.AuditLogger;
import com.blog.blogsystme.util.PageUtil;
import com.blog.blogsystme.util.RoleConst;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private static final Set<String> ALLOWED_FEEDBACK_STATUS = Set.of("pending", "resolved");

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final FeedbackMapper feedbackMapper;
    private final LikeMapper likeMapper;
    private final BookmarkMapper bookmarkMapper;
    private final FollowMapper followMapper;
    private final NotificationMapper notificationMapper;
    private final PasswordResetTokenMapper tokenMapper;

    @Value("${admin.bootstrap-username:admin}")
    private String bootstrapUsername;

    /** 管理员引导提升开关：默认关闭，显式设置 true 后才允许在无管理员时自动提升已注册的 admin 用户 */
    @Value("${admin.bootstrap-enabled:false}")
    private boolean bootstrapEnabled;

    public AdminController(UserMapper userMapper, ArticleMapper articleMapper,
                           CommentMapper commentMapper, FeedbackMapper feedbackMapper,
                           LikeMapper likeMapper, BookmarkMapper bookmarkMapper,
                           FollowMapper followMapper, NotificationMapper notificationMapper,
                           PasswordResetTokenMapper tokenMapper) {
        this.userMapper = userMapper;
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.feedbackMapper = feedbackMapper;
        this.likeMapper = likeMapper;
        this.bookmarkMapper = bookmarkMapper;
        this.followMapper = followMapper;
        this.notificationMapper = notificationMapper;
        this.tokenMapper = tokenMapper;
    }

    @PostConstruct
    public void bootstrapAdmin() {
        try {
            if (userMapper.countAdmins() > 0) return;
            if (!bootstrapEnabled) {
                log.warn("系统中无管理员且 admin.bootstrap-enabled 未开启，请通过数据库手动设置管理员: "
                        + "UPDATE user SET role='admin' WHERE username='你的用户名';");
                return;
            }
            User seed = userMapper.findByUsername(bootstrapUsername);
            if (seed != null) {
                userMapper.updateRole(seed.getId(), RoleConst.ADMIN);
                AuditLogger.log("ADMIN_BOOTSTRAP", "userId=" + seed.getId() + ", username=" + seed.getUsername());
                log.warn("系统中无管理员，已将用户 {} (id={}) 提升为管理员，请尽快登录确认",
                        seed.getUsername(), seed.getId());
            } else {
                log.warn("系统中无管理员且用户 {} 不存在，请先注册该用户名再开启引导提升，或通过数据库手动设置管理员",
                        bootstrapUsername);
            }
        } catch (Exception e) {
            log.warn("管理员初始化检查失败", e);
        }
    }

    private boolean checkAdmin(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) return false;
        User user = userMapper.findById(userId);
        return user != null && RoleConst.ADMIN.equals(user.getRole());
    }

    @GetMapping("/users")
    @Operation(summary = "用户列表（分页）")
    public ResponseEntity<ApiResponse<PageResponse>> users(HttpServletRequest request,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int pageSize) {
        if (!checkAdmin(request)) return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 100);
        java.util.List<User> users = userMapper.findByPage(PageUtil.start(p, size), size);
        users.forEach(u -> u.setPassword(null));
        int total = userMapper.countAll();
        return ResponseEntity.ok(ApiResponse.success("查询成功", new PageResponse(users, total, size, p)));
    }

    @DeleteMapping("/user/{id}")
    @Operation(summary = "删除用户(级联删除其全部数据)")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteUser(HttpServletRequest request, @PathVariable Integer id) {
        if (!checkAdmin(request)) return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        User user = userMapper.findById(id);
        if (user == null) return ResponseEntity.badRequest().body(ApiResponse.fail("用户不存在"));
        if (RoleConst.ADMIN.equals(user.getRole()))
            return ResponseEntity.badRequest().body(ApiResponse.fail("不能删除管理员"));
        commentMapper.deleteByUserId(id);
        articleMapper.deleteByUserId(id);
        likeMapper.deleteByUserId(id);
        bookmarkMapper.deleteByUserId(id);
        followMapper.deleteByUserId(id);
        notificationMapper.deleteByUserId(id);
        feedbackMapper.deleteByUserId(id);
        tokenMapper.deleteByUserId(id);
        userMapper.deleteById(id);
        AuditLogger.log("ADMIN_DELETE_USER", "operatorId=" + request.getAttribute("userId")
                + ", targetUserId=" + id + ", targetUsername=" + user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("删除成功（含该用户全部数据）"));
    }

    @GetMapping("/articles")
    @Operation(summary = "文章管理（分页）")
    public ResponseEntity<ApiResponse<PageResponse>> articles(HttpServletRequest request,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int pageSize) {
        if (!checkAdmin(request)) return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 100);
        return ResponseEntity.ok(ApiResponse.success("查询成功",
                new PageResponse(articleMapper.findAllAdminPage(PageUtil.start(p, size), size),
                        articleMapper.count(), size, p)));
    }

    @DeleteMapping("/article/{id}")
    @Operation(summary = "删除文章(级联清理)")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteArticle(HttpServletRequest request, @PathVariable Integer id) {
        if (!checkAdmin(request)) return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        likeMapper.deleteByArticleId(id);
        bookmarkMapper.deleteByArticleId(id);
        commentMapper.deleteByArticleId(id);
        notificationMapper.deleteByArticleId(id);
        articleMapper.deleteByAdmin(id);
        AuditLogger.log("ADMIN_DELETE_ARTICLE", "operatorId=" + request.getAttribute("userId")
                + ", articleId=" + id);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }

    @GetMapping("/feedback")
    @Operation(summary = "反馈列表（分页）")
    public ResponseEntity<ApiResponse<PageResponse>> feedback(HttpServletRequest request,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int pageSize) {
        if (!checkAdmin(request)) return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 100);
        return ResponseEntity.ok(ApiResponse.success("查询成功",
                new PageResponse(feedbackMapper.findAllWithUserPage(PageUtil.start(p, size), size),
                        feedbackMapper.countAll(), size, p)));
    }

    @PutMapping("/feedback/{id}")
    @Operation(summary = "处理反馈")
    public ResponseEntity<ApiResponse<Void>> handleFeedback(HttpServletRequest request,
                                                             @PathVariable Integer id,
                                                             @RequestBody Map<String, String> body) {
        if (!checkAdmin(request)) return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        String status = body.getOrDefault("status", "resolved");
        if (!ALLOWED_FEEDBACK_STATUS.contains(status)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("非法的反馈状态"));
        }
        int rows = feedbackMapper.updateStatus(id, status);
        if (rows == 0) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("反馈不存在"));
        }
        AuditLogger.log("ADMIN_HANDLE_FEEDBACK", "operatorId=" + request.getAttribute("userId")
                + ", feedbackId=" + id + ", status=" + status);
        return ResponseEntity.ok(ApiResponse.success("已处理"));
    }

    @GetMapping("/stats")
    @Operation(summary = "系统统计")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats(HttpServletRequest request) {
        if (!checkAdmin(request)) return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", userMapper.countAll());
        data.put("articles", articleMapper.count());
        data.put("comments", commentMapper.countAll());
        data.put("feedback", feedbackMapper.countAll());
        data.put("likes", likeMapper.countAll());
        return ResponseEntity.ok(ApiResponse.success("查询成功", data));
    }

    @PutMapping("/set-admin/{userId}")
    @Operation(summary = "设置管理员")
    public ResponseEntity<ApiResponse<Void>> setAdmin(HttpServletRequest request, @PathVariable Integer userId) {
        if (!checkAdmin(request)) return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        User target = userMapper.findById(userId);
        if (target == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("用户不存在"));
        }
        userMapper.updateRole(userId, RoleConst.ADMIN);
        AuditLogger.log("ADMIN_GRANT", "operatorId=" + request.getAttribute("userId")
                + ", targetUserId=" + userId + ", targetUsername=" + target.getUsername());
        return ResponseEntity.ok(ApiResponse.success("已设置为管理员"));
    }

    @PutMapping("/revoke-admin/{userId}")
    @Operation(summary = "撤销管理员")
    public ResponseEntity<ApiResponse<Void>> revokeAdmin(HttpServletRequest request, @PathVariable Integer userId) {
        if (!checkAdmin(request)) return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        User target = userMapper.findById(userId);
        if (target == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("用户不存在"));
        }
        Integer operatorId = (Integer) request.getAttribute("userId");
        if (operatorId.equals(userId)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("不能撤销自己的管理员权限"));
        }
        if (userMapper.countAdmins() <= 1) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("系统至少需要保留一名管理员"));
        }
        userMapper.updateRole(userId, RoleConst.USER);
        AuditLogger.log("ADMIN_REVOKE", "operatorId=" + request.getAttribute("userId")
                + ", targetUserId=" + userId + ", targetUsername=" + target.getUsername());
        return ResponseEntity.ok(ApiResponse.success("已撤销管理员权限"));
    }
}

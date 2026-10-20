package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ForgotPasswordRequest;
import com.blog.blogsystem.dto.ResetPasswordRequest;
import com.blog.blogsystem.entity.PasswordResetToken;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.PasswordResetTokenMapper;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.service.PasswordRecoveryService;
import com.blog.blogsystem.util.AuditLogger;
import com.blog.blogsystem.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 密码找回。
 *
 * 安全约束（相对旧实现的三处收紧）：
 * 1. 只接受邮箱作为账号标识：手机号没有经过 OTP 验证，不能作为找回凭据，
 *    否则"知道手机号"就等于"拥有账号"。
 * 2. 不再要求/信任客户端提交的 verify 字段：持有性证明只能通过
 *    "收得到邮件里的令牌"来完成，而不是"能说出邮箱/手机号"。
 * 3. 令牌默认只经邮件带外传递；只有在显式开启
 *    app.security.allow-dev-reset-token=true 且处于 dev/local/test profile 时，
 *    才允许在响应中返回令牌（本机联调用），默认关闭。
 */
@Service
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_EXPIRE_MINUTES = 30;

    /** 统一的对外文案：账号不存在、未绑定邮箱、邮箱重复时返回同一句，避免账号枚举 */
    private static final String GENERIC_RESET_MSG = "如果该账号存在且已绑定邮箱，重置指引已发送，请查收";
    private static final String GENERIC_USERNAME_MSG = "如果该邮箱已注册，账号信息将发送至该邮箱";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final UserMapper userMapper;
    private final PasswordResetTokenMapper tokenMapper;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Environment environment;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.reset-password-url:http://localhost:5173/reset-password}")
    private String resetPasswordUrl;

    /** 仅本机联调使用：默认 false，生产永远不要打开 */
    @Value("${app.security.allow-dev-reset-token:false}")
    private boolean allowDevResetToken;

    public PasswordRecoveryServiceImpl(UserMapper userMapper,
                                        PasswordResetTokenMapper tokenMapper,
                                        ObjectProvider<JavaMailSender> mailSenderProvider,
                                        Environment environment) {
        this.userMapper = userMapper;
        this.tokenMapper = tokenMapper;
        this.mailSenderProvider = mailSenderProvider;
        this.environment = environment;
    }

    /**
     * 找回用户名：不再把用户名列表回给调用方（那样就是一个"邮箱是否注册"的探测接口），
     * 改为命中后发邮件，响应恒为同一句提示。
     */
    @Override
    public ApiResponse<List<String>> findUsernamesByEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return ApiResponse.success(GENERIC_USERNAME_MSG, List.of());
        }
        String target = email.trim();
        List<User> users = userMapper.findByEmail(target);
        if (users.isEmpty()) {
            AuditLogger.log("USERNAME_LOOKUP_MISS", "email=" + target);
            return ApiResponse.success(GENERIC_USERNAME_MSG, List.of());
        }
        if (users.size() > 1) {
            // 历史数据里同邮箱多账号：不猜测归属，只记审计
            AuditLogger.log("USERNAME_LOOKUP_AMBIGUOUS", "email=" + target + ", count=" + users.size());
            return ApiResponse.success(GENERIC_USERNAME_MSG, List.of());
        }
        sendUsernameMail(users.get(0));
        AuditLogger.log("USERNAME_LOOKUP_SENT", "userId=" + users.get(0).getId());
        return ApiResponse.success(GENERIC_USERNAME_MSG, List.of());
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, Object>> requestReset(ForgotPasswordRequest request) {
        String account = request.getAccount() == null ? "" : request.getAccount().trim();

        // 手机号不再作为找回凭据（无 OTP 验证 = 无持有性证明）
        if (!EMAIL_PATTERN.matcher(account).matches()) {
            AuditLogger.log("RESET_REQUEST_REJECT", "reason=non_email_account, account=" + account);
            return ApiResponse.success(GENERIC_RESET_MSG, Map.of("emailSent", false));
        }

        List<User> users = userMapper.findByEmail(account);
        if (users == null || users.isEmpty()) {
            AuditLogger.log("RESET_REQUEST_UNKNOWN", "account=" + account);
            return ApiResponse.success(GENERIC_RESET_MSG, Map.of("emailSent", false));
        }
        if (users.size() > 1) {
            // 邮箱不唯一的历史数据：拒绝而不是随便挑一个，避免重置到他人账号
            AuditLogger.log("RESET_REQUEST_AMBIGUOUS", "account=" + account + ", count=" + users.size());
            log.error("邮箱绑定到多个账号，拒绝密码重置: account={}, count={}", account, users.size());
            return ApiResponse.success(GENERIC_RESET_MSG, Map.of("emailSent", false));
        }

        User user = users.get(0);
        tokenMapper.invalidateAllForUser(user.getId());

        byte[] rawToken = new byte[32];
        RANDOM.nextBytes(rawToken);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken);
        String tokenHash = sha256(token);

        PasswordResetToken entity = new PasswordResetToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(tokenHash);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRE_MINUTES));
        tokenMapper.insert(entity);
        AuditLogger.log("RESET_REQUESTED", "userId=" + user.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expiresInMinutes", TOKEN_EXPIRE_MINUTES);
        result.put("emailSent", false);

        if (sendResetMail(user, token)) {
            result.put("emailSent", true);
            return ApiResponse.success(GENERIC_RESET_MSG, result);
        }

        if (allowDevResetToken && isDevEnvironment()) {
            // 显式开关 + 非生产 profile 才允许，缺一不可
            result.put("token", token);
            log.warn("开发模式返回重置令牌（app.security.allow-dev-reset-token=true）: userId={}", user.getId());
            return ApiResponse.success("开发模式：重置令牌已返回", result);
        }

        log.error("SMTP 未配置或发送失败，且未开启开发令牌开关，拒绝返回令牌: userId={}", user.getId());
        return ApiResponse.success(GENERIC_RESET_MSG, result);
    }

    private boolean sendResetMail(User user, String token) {
        if (mailHost == null || mailHost.isBlank() || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) return false;
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(user.getEmail());
            mail.setSubject("【智能博客】密码重置");
            mail.setText("您好 " + user.getUsername() + "：\n\n"
                    + "请在 " + TOKEN_EXPIRE_MINUTES + " 分钟内通过以下链接重置密码：\n"
                    + resetPasswordUrl + "?token=" + token + "\n\n"
                    + "若非本人操作，请忽略此邮件，并考虑修改密码。");
            mailSender.send(mail);
            return true;
        } catch (Exception e) {
            log.warn("重置邮件发送失败: {}", e.getMessage());
            return false;
        }
    }

    private void sendUsernameMail(User user) {
        if (mailHost == null || mailHost.isBlank() || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) return;
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(user.getEmail());
            mail.setSubject("【智能博客】账号找回");
            mail.setText("您好：\n\n该邮箱在本站绑定的用户名是：" + user.getUsername() + "\n\n"
                    + "若非本人操作，请忽略此邮件。");
            mailSender.send(mail);
        } catch (Exception e) {
            log.warn("账号找回邮件发送失败: {}", e.getMessage());
        }
    }

    private boolean isDevEnvironment() {
        String[] profiles = environment.getActiveProfiles();
        return Arrays.stream(profiles)
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("local") || p.equalsIgnoreCase("test"));
    }

    @Override
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        String tokenHash = sha256(request.getToken().trim());

        PasswordResetToken token = tokenMapper.findByHash(tokenHash);
        if (token == null) {
            return ApiResponse.fail("重置令牌无效");
        }
        if (Boolean.TRUE.equals(token.getUsed())) {
            return ApiResponse.fail("重置令牌已被使用");
        }
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ApiResponse.fail("重置令牌已过期，请重新申请");
        }

        User user = userMapper.findById(token.getUserId());
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        userMapper.updatePassword(user.getId(), PasswordUtil.encode(request.getNewPassword()));
        userMapper.incrementTokenVersion(user.getId());
        tokenMapper.markUsed(token.getId());
        tokenMapper.invalidateAllForUser(user.getId());

        AuditLogger.log("RESET_COMPLETED", "userId=" + user.getId());
        log.info("密码重置成功: userId={}", user.getId());
        return ApiResponse.success("密码重置成功，请使用新密码登录");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}

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
import com.blog.blogsystem.util.MaskUtil;
import com.blog.blogsystem.util.PasswordUtil;
import com.blog.blogsystem.util.RateLimiterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 密码找回实现：令牌 SHA-256 哈希存储、仅邮件投递重置链接、防账号枚举
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Service
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_EXPIRE_MINUTES = 30;

    private final UserMapper userMapper;
    private final PasswordResetTokenMapper tokenMapper;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.reset-password-url:http://localhost:5173/reset-password}")
    private String resetPasswordUrl;

    public PasswordRecoveryServiceImpl(UserMapper userMapper,
                                        PasswordResetTokenMapper tokenMapper,
                                        ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.userMapper = userMapper;
        this.tokenMapper = tokenMapper;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public ApiResponse<List<String>> findUsernamesByEmail(String email) {
        // M3：账号提示只发往邮箱本身，响应恒为空列表，避免通过返回内容枚举账号
        List<User> users = userMapper.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (!users.isEmpty() && isMailConfigured()) {
            try {
                StringBuilder body = new StringBuilder("您的账号：\n");
                for (User u : users) {
                    body.append(u.getUsername()).append('\n');
                }
                body.append("\n若非本人操作，请忽略此邮件。");
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(email);
                mail.setSubject("【智能博客】账号找回");
                mail.setText(body.toString());
                mailSenderProvider.getIfAvailable().send(mail);
                AuditLogger.log("USERNAME_REMINDER_SENT", "count=" + users.size());
            } catch (Exception e) {
                log.error("账号提示邮件发送失败: error={}", e.getMessage());
            }
        }
        return ApiResponse.success("如果该邮箱已注册，账号提示已发送", List.of());
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, Object>> requestReset(ForgotPasswordRequest request) {
        String account = request.getAccount() == null ? "" : request.getAccount().trim();

        // 无论账号是否存在、是否绑定邮箱、邮件是否发送成功，响应体完全一致，防止账号枚举
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expiresInMinutes", TOKEN_EXPIRE_MINUTES);
        result.put("emailSent", true);
        String genericMessage = "如果账号存在且已绑定邮箱，重置链接已发送";

        User user = userMapper.findByUsername(account);
        if (user == null && account.contains("@")) {
            List<User> byEmail = userMapper.findByEmail(account.toLowerCase(Locale.ROOT));
            user = byEmail.isEmpty() ? null : byEmail.get(0);
        }
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            AuditLogger.log("RESET_REQUEST_IGNORED", "account="
                    + (account.contains("@") ? MaskUtil.maskEmail(account) : MaskUtil.maskUsername(account)));
            return ApiResponse.success(genericMessage, result);
        }

        // 同一账号每分钟最多申请 3 次，防止借找回接口轰炸邮箱/消耗资源
        if (RateLimiterUtil.isBlocked("reset:user:" + user.getId(), 3)) {
            AuditLogger.log("RESET_RATE_LIMITED", "userId=" + user.getId());
            return ApiResponse.success(genericMessage, result);
        }

        if (!isMailConfigured()) {
            log.error("SMTP 未配置，密码重置邮件无法发送: userId={}", user.getId());
            return ApiResponse.success(genericMessage, result);
        }

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

        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(user.getEmail());
            mail.setSubject("【智能博客】密码重置");
            mail.setText("您好 " + user.getUsername() + "：\n\n"
                    + "请在 " + TOKEN_EXPIRE_MINUTES + " 分钟内通过以下链接重置密码：\n"
                    + resetPasswordUrl + "?token=" + token + "\n\n"
                    + "若非本人操作，请忽略此邮件。");
            mailSender.send(mail);
            AuditLogger.log("RESET_REQUESTED", "userId=" + user.getId() + ", channel=email");
            log.info("密码重置邮件已发送: userId={}", user.getId());
        } catch (Exception e) {
            log.error("重置邮件发送失败: userId={}, error={}", user.getId(), e.getMessage());
        }

        // 令牌只经邮件传递，任何情况下都不进入响应体
        return ApiResponse.success(genericMessage, result);
    }

    private boolean isMailConfigured() {
        return mailHost != null && !mailHost.isBlank()
                && mailSenderProvider.getIfAvailable() != null;
    }

    @Scheduled(fixedRate = 3_600_000)
    public void cleanExpiredTokens() {
        try {
            int deleted = tokenMapper.deleteExpired();
            if (deleted > 0) {
                log.info("清理过期密码重置令牌: {} 条", deleted);
            }
        } catch (Exception e) {
            log.warn("清理过期密码重置令牌失败", e);
        }
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

        if (!PasswordUtil.isBcryptCompatible(request.getNewPassword())) {
            return ApiResponse.fail("新密码过长，UTF-8 编码后不能超过 72 字节");
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

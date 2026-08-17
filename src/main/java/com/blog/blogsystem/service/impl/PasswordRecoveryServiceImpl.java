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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_EXPIRE_MINUTES = 30;

    private final UserMapper userMapper;
    private final PasswordResetTokenMapper tokenMapper;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Environment environment;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.reset-password-url:http://localhost:5173/reset-password}")
    private String resetPasswordUrl;

    public PasswordRecoveryServiceImpl(UserMapper userMapper,
                                        PasswordResetTokenMapper tokenMapper,
                                        ObjectProvider<JavaMailSender> mailSenderProvider,
                                        Environment environment) {
        this.userMapper = userMapper;
        this.tokenMapper = tokenMapper;
        this.mailSenderProvider = mailSenderProvider;
        this.environment = environment;
    }

    @Override
    public ApiResponse<List<String>> findUsernamesByEmail(String email) {
        List<User> users = userMapper.findByEmail(email);
        if (users.isEmpty()) {
            return ApiResponse.success("查询成功", List.of());
        }
        List<String> masked = new ArrayList<>();
        for (User u : users) {
            masked.add(MaskUtil.maskUsername(u.getUsername()));
        }
        return ApiResponse.success("查询成功", masked);
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, Object>> requestReset(ForgotPasswordRequest request) {
        String account = request.getAccount().trim();
        String verify = request.getVerify().trim();

        User user;
        if (account.matches("\\d{11}")) {
            user = userMapper.findByPhone(account);
        } else {
            user = userMapper.findByUsername(account);
        }

        if (user == null) {
            return ApiResponse.fail("账号不存在或未绑定该邮箱/手机号");
        }

        boolean verified = verify.equals(user.getEmail()) || verify.equals(user.getPhone());
        if (!verified) {
            return ApiResponse.fail("账号不存在或未绑定该邮箱/手机号");
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
        AuditLogger.log("RESET_REQUESTED", "userId=" + user.getId() + ", account=" + account);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expiresInMinutes", TOKEN_EXPIRE_MINUTES);

        if (mailHost != null && !mailHost.isBlank()) {
            try {
                JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
                if (mailSender != null) {
                    SimpleMailMessage mail = new SimpleMailMessage();
                    mail.setTo(user.getEmail());
                    mail.setSubject("【智能博客】密码重置");
                    mail.setText("您好 " + user.getUsername() + "：\n\n"
                            + "请在 " + TOKEN_EXPIRE_MINUTES + " 分钟内通过以下链接重置密码：\n"
                            + resetPasswordUrl + "?token=" + token + "\n\n"
                            + "若非本人操作，请忽略此邮件。");
                    mailSender.send(mail);
                    result.put("emailSent", true);
                    return ApiResponse.success("重置链接已发送至绑定邮箱", result);
                }
            } catch (Exception e) {
                log.warn("重置邮件发送失败，降级为响应返回: {}", e.getMessage());
            }
        }

        result.put("emailSent", false);
        if (!isDevEnvironment()) {
            // 生产环境 SMTP 未配置时绝不通过 API 返回令牌，防止账号接管
            log.error("SMTP 未配置且非开发环境，拒绝返回重置令牌: userId={}", user.getId());
            return ApiResponse.fail("邮件服务未配置，请联系管理员");
        }
        result.put("token", token);
        log.warn("SMTP 未配置，重置令牌通过响应返回（仅限开发环境）: userId={}", user.getId());
        return ApiResponse.success("SMTP 未配置，重置令牌已返回（开发模式）", result);
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

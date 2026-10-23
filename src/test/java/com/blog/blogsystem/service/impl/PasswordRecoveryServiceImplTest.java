package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ForgotPasswordRequest;
import com.blog.blogsystem.entity.PasswordResetToken;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.PasswordResetTokenMapper;
import com.blog.blogsystem.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordResetTokenMapper tokenMapper;
    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    private PasswordRecoveryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PasswordRecoveryServiceImpl(userMapper, tokenMapper, mailSenderProvider);
        ReflectionTestUtils.setField(service, "mailHost", "smtp.example.com");
        ReflectionTestUtils.setField(service, "resetPasswordUrl", "https://example.com/reset-password");
    }

    @Test
    void requestResetShouldSendEmailButNeverReturnToken() {
        User user = new User();
        user.setId(1);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        when(userMapper.findByUsername("alice")).thenReturn(user);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(tokenMapper.insert(any(PasswordResetToken.class))).thenReturn(1);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setAccount("alice");

        ApiResponse<Map<String, Object>> response = service.requestReset(request);

        assertTrue(response.isSuccess());
        assertFalse(response.getData().containsKey("token"), "重置令牌绝不能进入响应体");
        assertEquals(true, response.getData().get("emailSent"));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void unknownAccountShouldReturnGenericMessageWithoutSendingEmail() {
        when(userMapper.findByUsername("nobody")).thenReturn(null);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setAccount("nobody");

        ApiResponse<Map<String, Object>> response = service.requestReset(request);

        assertTrue(response.isSuccess());
        assertFalse(response.getData().containsKey("token"));
        verifyNoInteractions(mailSender);
    }
}

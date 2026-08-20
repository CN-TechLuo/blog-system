package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.service.CaptchaService;
import com.blog.blogsystem.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blog.blogsystem.dto.ChangePasswordRequest;

class UserServiceImplTest {

    private UserMapper mapper = mock(UserMapper.class);
    private TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
    private CaptchaService captchaService = mock(CaptchaService.class);
    private UserServiceImpl service = new UserServiceImpl(mapper, captchaService);

    @Test
    void changePasswordShouldRejectWrongOldPassword() {
        User user = new User();
        user.setId(1);
        user.setPassword(com.blog.blogsystem.util.PasswordUtil.encode("OldPass@123"));
        when(mapper.findById(1)).thenReturn(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("WrongPass@123");
        request.setNewPassword("NewPass@1234");

        var result = service.changePassword(1, request);
        assertFalse(result.isSuccess());
        assertEquals("原密码错误", result.getMessage());
        verify(mapper, never()).updatePassword(anyInt(), anyString());
    }

    @Test
    void changePasswordShouldSucceedAndInvalidateTokens() {
        User user = new User();
        user.setId(1);
        user.setPassword(com.blog.blogsystem.util.PasswordUtil.encode("OldPass@123"));
        when(mapper.findById(1)).thenReturn(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass@123");
        request.setNewPassword("NewPass@1234");

        var result = service.changePassword(1, request);
        assertTrue(result.isSuccess());
        verify(mapper).updatePassword(eq(1), anyString());
        verify(mapper).incrementTokenVersion(1);
    }
}

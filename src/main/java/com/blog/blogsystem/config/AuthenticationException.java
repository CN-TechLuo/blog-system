package com.blog.blogsystem.config;

import org.springframework.http.HttpStatus;

/**
 * 认证异常，AuthInterceptor 中抛出，由 GlobalExceptionHandler 统一处理
 */
public class AuthenticationException extends RuntimeException {

    private final int status;
    private final String message;

    public AuthenticationException(int status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public AuthenticationException(HttpStatus status, String message) {
        this(status.value(), message);
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

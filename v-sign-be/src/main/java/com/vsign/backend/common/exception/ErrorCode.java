package com.vsign.backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Account is disabled"),
    PREMIUM_REQUIRED(HttpStatus.FORBIDDEN, "Premium account required"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    ATTEMPT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "Attempt already submitted"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    UNIT_NOT_FOUND(HttpStatus.NOT_FOUND, "Unit not found"),
    CHAPTER_NOT_FOUND(HttpStatus.NOT_FOUND, "Chapter not found"),
    LESSON_NOT_FOUND(HttpStatus.NOT_FOUND, "Lesson not found"),
    ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "Attempt not found"),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}

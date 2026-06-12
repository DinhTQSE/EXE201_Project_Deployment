package com.vsign.backend.common.response;

import java.time.OffsetDateTime;

public record SuccessResponse<T>(
        boolean success,
        String message,
        T data,
        OffsetDateTime timestamp
) {
    public static <T> SuccessResponse<T> ok(String message, T data) {
        return new SuccessResponse<>(true, message, data, OffsetDateTime.now());
    }

    public static <T> SuccessResponse<T> created(String message, T data) {
        return new SuccessResponse<>(true, message, data, OffsetDateTime.now());
    }
}

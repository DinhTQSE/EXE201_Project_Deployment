package com.vsign.backend.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        int status,
        String code,
        String message,
        List<ValidationError> validationErrors,
        OffsetDateTime timestamp
) {
    public static ApiErrorResponse of(ErrorCode code, String message) {
        return new ApiErrorResponse(false, code.status().value(), code.name(), message, List.of(), OffsetDateTime.now());
    }

    public static ApiErrorResponse validation(List<ValidationError> errors) {
        return new ApiErrorResponse(
                false,
                ErrorCode.VALIDATION_ERROR.status().value(),
                ErrorCode.VALIDATION_ERROR.name(),
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                errors,
                OffsetDateTime.now()
        );
    }

    public record ValidationError(String field, String message) {
    }
}

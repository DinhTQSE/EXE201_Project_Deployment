package com.vsign.backend.common.exception;

import java.util.List;

public class FieldValidationException extends RuntimeException {
    private final List<ApiErrorResponse.ValidationError> validationErrors;

    public FieldValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_ERROR.defaultMessage());
        this.validationErrors = List.of(new ApiErrorResponse.ValidationError(field, message));
    }

    public List<ApiErrorResponse.ValidationError> validationErrors() {
        return validationErrors;
    }
}

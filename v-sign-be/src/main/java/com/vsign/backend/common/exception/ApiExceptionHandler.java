package com.vsign.backend.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException exception) {
        ErrorCode code = exception.errorCode();
        return ResponseEntity.status(code.status()).body(ApiErrorResponse.of(code, exception.getMessage()));
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleFieldValidation(FieldValidationException exception) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.validation(exception.validationErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.ValidationError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.ValidationError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiErrorResponse.ValidationError::field))
                .toList();
        return ResponseEntity.badRequest().body(ApiErrorResponse.validation(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiErrorResponse.ValidationError> errors = exception.getConstraintViolations()
                .stream()
                .map(error -> new ApiErrorResponse.ValidationError(error.getPropertyPath().toString(), error.getMessage()))
                .sorted(Comparator.comparing(ApiErrorResponse.ValidationError::field))
                .toList();
        return ResponseEntity.badRequest().body(ApiErrorResponse.validation(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage()));
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ApiErrorResponse> handleAuthentication(Exception exception) {
        return ResponseEntity.status(ErrorCode.UNAUTHORIZED.status())
                .body(ApiErrorResponse.of(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.defaultMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.status())
                .body(ApiErrorResponse.of(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage()));
    }
}

package com.vsign.backend.auth.controller;

import com.vsign.backend.auth.dto.AuthResponse;
import com.vsign.backend.auth.dto.PasswordResetCompleteRequest;
import com.vsign.backend.auth.dto.PasswordResetRequest;
import com.vsign.backend.auth.dto.LoginRequest;
import com.vsign.backend.auth.dto.RegisterRequest;
import com.vsign.backend.auth.service.AuthService;
import com.vsign.backend.common.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return SuccessResponse.created("Registration successful", authService.register(request));
    }

    @PostMapping("/login")
    public SuccessResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return SuccessResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/password-reset/request")
    public SuccessResponse<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return SuccessResponse.ok("Password reset requested", null);
    }

    @PostMapping("/password-reset/complete")
    public SuccessResponse<Void> completePasswordReset(@Valid @RequestBody PasswordResetCompleteRequest request) {
        authService.completePasswordReset(request);
        return SuccessResponse.ok("Password reset successfully completed", null);
    }
}

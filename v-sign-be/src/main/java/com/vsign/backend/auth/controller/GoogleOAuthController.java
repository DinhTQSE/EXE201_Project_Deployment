package com.vsign.backend.auth.controller;

import com.vsign.backend.auth.service.GoogleOAuthService;
import com.vsign.backend.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    @GetMapping("/login-url")
    public SuccessResponse<String> getLoginUrl() {
        return SuccessResponse.ok("Google login URL generated", googleOAuthService.getAuthorizationUrl());
    }

    @GetMapping("/callback")
    public void handleCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        String redirectUrl = googleOAuthService.handleCallbackAndGetRedirect(code);
        response.sendRedirect(redirectUrl);
    }
}

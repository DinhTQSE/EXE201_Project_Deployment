package com.vsign.backend.auth.controller;

import com.vsign.backend.auth.dto.ChangePasswordRequest;
import com.vsign.backend.auth.dto.ProfileResponse;
import com.vsign.backend.auth.dto.UpdateProfileRequest;
import com.vsign.backend.auth.service.ProfileService;
import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.common.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public SuccessResponse<ProfileResponse> getProfile(@AuthenticationPrincipal JwtService.Principal principal) {
        return SuccessResponse.ok("Profile loaded", profileService.getProfile(principal.email()));
    }

    @PatchMapping("/profile")
    public SuccessResponse<ProfileResponse> updateProfile(
            @AuthenticationPrincipal JwtService.Principal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return SuccessResponse.ok("Profile updated", profileService.updateProfile(principal.email(), request));
    }

    @PostMapping("/change-password")
    public SuccessResponse<Void> changePassword(
            @AuthenticationPrincipal JwtService.Principal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        profileService.changePassword(principal.email(), request);
        return SuccessResponse.ok("Password changed", null);
    }
}

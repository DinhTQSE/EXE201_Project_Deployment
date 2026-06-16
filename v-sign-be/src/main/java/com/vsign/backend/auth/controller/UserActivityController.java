package com.vsign.backend.auth.controller;

import com.vsign.backend.auth.dto.ActivityHeartbeatRequest;
import com.vsign.backend.auth.service.UserActivityService;
import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.common.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/activity")
public class UserActivityController {
    private final UserActivityService userActivityService;

    public UserActivityController(UserActivityService userActivityService) {
        this.userActivityService = userActivityService;
    }

    @PostMapping("/heartbeat")
    public SuccessResponse<Void> heartbeat(
            @AuthenticationPrincipal JwtService.Principal principal,
            @Valid @RequestBody(required = false) ActivityHeartbeatRequest request
    ) {
        userActivityService.recordHeartbeat(
                principal.email(),
                request == null ? null : request.activeSeconds()
        );
        return SuccessResponse.ok("Activity heartbeat recorded", null);
    }
}

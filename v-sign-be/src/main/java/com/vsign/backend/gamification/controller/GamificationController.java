package com.vsign.backend.gamification.controller;

import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.gamification.dto.UserProgressSummaryResponse;
import com.vsign.backend.gamification.dto.XpAwardRequest;
import com.vsign.backend.gamification.dto.XpAwardResponse;
import com.vsign.backend.gamification.service.GamificationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gamification")
public class GamificationController {
    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/summary")
    public SuccessResponse<UserProgressSummaryResponse> summary(@AuthenticationPrincipal JwtService.Principal principal) {
        return SuccessResponse.ok("Gamification summary loaded", gamificationService.summary(principal.email()));
    }

    @PostMapping("/xp-awards")
    public SuccessResponse<XpAwardResponse> awardXp(
            @AuthenticationPrincipal JwtService.Principal principal,
            @Valid @RequestBody XpAwardRequest request
    ) {
        return SuccessResponse.ok("XP awarded", gamificationService.awardXp(principal.email(), request));
    }
}

package com.vsign.backend.gamification.controller;

import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.gamification.dto.LeaderboardPeriod;
import com.vsign.backend.gamification.dto.LeaderboardResponse;
import com.vsign.backend.gamification.service.GamificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class LeaderboardController {
    private final GamificationService gamificationService;

    public LeaderboardController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/api/v1/leaderboards")
    public SuccessResponse<LeaderboardResponse> leaderboard(
            @AuthenticationPrincipal JwtService.Principal principal,
            @RequestParam(defaultValue = "WEEKLY") LeaderboardPeriod period,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return SuccessResponse.ok("Leaderboard loaded", gamificationService.leaderboard(principal.email(), period, page, size));
    }
}

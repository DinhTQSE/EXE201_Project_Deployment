package com.vsign.backend.gamification.dto;

import java.util.List;

public record LeaderboardResponse(
        LeaderboardPeriod period,
        int page,
        int size,
        List<LeaderboardEntryResponse> entries,
        LeaderboardEntryResponse currentUser
) {
}

package com.vsign.backend.gamification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record XpAwardRequest(
        @NotBlank
        String eventId,

        @NotBlank
        String source,

        @Min(1)
        int xpDelta,

        LocalDate activityDate
) {
}

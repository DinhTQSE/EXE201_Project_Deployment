package com.vsign.backend.monetization.dto;

public record ActivePlanResponse(
        String planId,
        String status,
        String endDate
) {
}

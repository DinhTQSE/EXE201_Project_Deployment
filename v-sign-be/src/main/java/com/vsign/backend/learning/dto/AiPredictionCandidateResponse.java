package com.vsign.backend.learning.dto;

public record AiPredictionCandidateResponse(
        String label,
        Double confidence
) {
}

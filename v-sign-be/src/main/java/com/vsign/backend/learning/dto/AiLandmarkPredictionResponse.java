package com.vsign.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiLandmarkPredictionResponse(
        String status,
        String label,
        Double confidence,
        List<AiPredictionCandidateResponse> top3,

        @JsonProperty("frames_processed")
        Integer framesProcessed,

        @JsonProperty("hands_detected_frames")
        Integer handsDetectedFrames,

        String message,

        @JsonProperty("inference_ms")
        Double inferenceMs,

        @JsonProperty("model_version")
        String modelVersion,

        @JsonProperty("label_version")
        String labelVersion
) {
}

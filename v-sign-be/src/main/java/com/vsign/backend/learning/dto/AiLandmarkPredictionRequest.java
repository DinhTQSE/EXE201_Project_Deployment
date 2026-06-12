package com.vsign.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiLandmarkPredictionRequest(
        @NotNull
        @Size(min = 5, max = 120)
        List<@NotNull @Size(min = 258, max = 258) List<@NotNull Double>> sequence,

        @JsonProperty("hands_detected_frames")
        @Min(0)
        @Max(120)
        Integer handsDetectedFrames,

        String targetGloss
) {
}

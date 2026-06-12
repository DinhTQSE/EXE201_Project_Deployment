package com.vsign.backend.assessment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionResponse(
        String id,
        String prompt,
        List<OptionResponse> options,
        String correctAnswerId
) {
}

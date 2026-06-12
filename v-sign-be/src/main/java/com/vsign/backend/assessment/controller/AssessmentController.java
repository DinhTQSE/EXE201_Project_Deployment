package com.vsign.backend.assessment.controller;

import com.vsign.backend.assessment.dto.AssessmentDetailResponse;
import com.vsign.backend.assessment.dto.AssessmentSubmissionRequest;
import com.vsign.backend.assessment.dto.AssessmentSubmissionResultResponse;
import com.vsign.backend.assessment.dto.AssessmentSummaryResponse;
import com.vsign.backend.assessment.service.AssessmentService;
import com.vsign.backend.common.response.SuccessResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {
    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public SuccessResponse<List<AssessmentSummaryResponse>> list() {
        return SuccessResponse.ok("Assessments loaded", assessmentService.listAssessments());
    }

    @GetMapping("/{assessmentId}")
    public SuccessResponse<AssessmentDetailResponse> detail(@PathVariable String assessmentId) {
        return SuccessResponse.ok("Assessment loaded", assessmentService.getAssessment(assessmentId));
    }

    @PostMapping("/{assessmentId}/submissions")
    public SuccessResponse<AssessmentSubmissionResultResponse> submit(
            @PathVariable String assessmentId,
            @Valid @RequestBody AssessmentSubmissionRequest request
    ) {
        return SuccessResponse.ok("Assessment submitted", assessmentService.submit(assessmentId, request));
    }
}

package com.vsign.backend.learning.controller;

import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.learning.dto.AiLandmarkPredictionRequest;
import com.vsign.backend.learning.dto.AiLandmarkPredictionResponse;
import com.vsign.backend.learning.dto.SignatureAttemptResponse;
import com.vsign.backend.learning.dto.SubmitSignatureAttemptRequest;
import com.vsign.backend.learning.service.AiPredictionProxyService;
import com.vsign.backend.learning.service.LearningWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/signature-workflows")
public class SignatureWorkflowController {
    private final LearningWorkflowService learningWorkflowService;
    private final AiPredictionProxyService aiPredictionProxyService;

    public SignatureWorkflowController(
            LearningWorkflowService learningWorkflowService,
            AiPredictionProxyService aiPredictionProxyService
    ) {
        this.learningWorkflowService = learningWorkflowService;
        this.aiPredictionProxyService = aiPredictionProxyService;
    }

    @PostMapping("/predict-landmarks")
    public SuccessResponse<AiLandmarkPredictionResponse> predictLandmarks(
            @Valid @RequestBody AiLandmarkPredictionRequest request
    ) {
        return SuccessResponse.ok("AI prediction completed", aiPredictionProxyService.predictLandmarks(request));
    }

    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<SignatureAttemptResponse> submit(@Valid @RequestBody SubmitSignatureAttemptRequest request) {
        return SuccessResponse.created("Signature attempt submitted", learningWorkflowService.submitSignatureAttempt(request));
    }
}

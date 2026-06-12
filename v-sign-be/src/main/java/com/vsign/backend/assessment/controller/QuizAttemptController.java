package com.vsign.backend.assessment.controller;

import com.vsign.backend.assessment.dto.QuizResultResponse;
import com.vsign.backend.assessment.dto.QuizReviewResponse;
import com.vsign.backend.assessment.dto.SubmitAttemptRequest;
import com.vsign.backend.assessment.service.QuizAttemptService;
import com.vsign.backend.common.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quiz-attempts")
public class QuizAttemptController {
    private final QuizAttemptService quizAttemptService;

    public QuizAttemptController(QuizAttemptService quizAttemptService) {
        this.quizAttemptService = quizAttemptService;
    }

    @PostMapping("/{attemptId}/submit")
    public SuccessResponse<QuizResultResponse> submit(
            @PathVariable String attemptId,
            @Valid @RequestBody SubmitAttemptRequest request
    ) {
        return SuccessResponse.ok("Quiz submitted", quizAttemptService.submit(attemptId, request));
    }

    @GetMapping("/{attemptId}/review")
    public SuccessResponse<QuizReviewResponse> review(@PathVariable String attemptId) {
        return SuccessResponse.ok("Quiz review loaded", quizAttemptService.review(attemptId));
    }
}

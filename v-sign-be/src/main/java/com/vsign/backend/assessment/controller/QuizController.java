package com.vsign.backend.assessment.controller;

import com.vsign.backend.assessment.dto.QuizResponse;
import com.vsign.backend.assessment.service.QuizAttemptService;
import com.vsign.backend.common.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuizController {
    private final QuizAttemptService quizAttemptService;

    public QuizController(QuizAttemptService quizAttemptService) {
        this.quizAttemptService = quizAttemptService;
    }

    @GetMapping("/api/v1/lessons/{lessonId}/quiz")
    public SuccessResponse<QuizResponse> getLessonQuiz(@PathVariable String lessonId) {
        return SuccessResponse.ok("Quiz loaded", quizAttemptService.getLessonQuiz(lessonId));
    }
}

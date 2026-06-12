package com.vsign.backend.learning.controller;

import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.learning.dto.ChapterListResponse;
import com.vsign.backend.learning.dto.LessonDetailResponse;
import com.vsign.backend.learning.dto.LessonListResponse;
import com.vsign.backend.learning.dto.ProgressResponse;
import com.vsign.backend.learning.dto.UnitListResponse;
import com.vsign.backend.learning.dto.UpdateProgressRequest;
import com.vsign.backend.learning.service.LearningWorkflowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class LearningCatalogController {
    private final LearningWorkflowService learningWorkflowService;

    public LearningCatalogController(LearningWorkflowService learningWorkflowService) {
        this.learningWorkflowService = learningWorkflowService;
    }

    @GetMapping("/api/v1/units")
    public SuccessResponse<UnitListResponse> listUnits(
            @RequestParam(defaultValue = "true") boolean publishedOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return SuccessResponse.ok("Units loaded", learningWorkflowService.listUnits(publishedOnly, page, size));
    }

    @GetMapping("/api/v1/units/{unitId}/chapters")
    public SuccessResponse<ChapterListResponse> listChapters(@PathVariable String unitId) {
        return SuccessResponse.ok("Chapters loaded", learningWorkflowService.listChapters(unitId));
    }

    @GetMapping("/api/v1/chapters/{chapterId}/lessons")
    public SuccessResponse<LessonListResponse> listLessons(@PathVariable String chapterId) {
        return SuccessResponse.ok("Lessons loaded", learningWorkflowService.listLessons(chapterId));
    }

    @GetMapping("/api/v1/lessons/{lessonId}")
    public SuccessResponse<LessonDetailResponse> getLesson(@PathVariable String lessonId) {
        return SuccessResponse.ok("Lesson loaded", learningWorkflowService.getLesson(lessonId));
    }

    @PutMapping("/api/v1/lessons/{lessonId}/progress")
    public SuccessResponse<ProgressResponse> updateProgress(
            @PathVariable String lessonId,
            @Valid @RequestBody UpdateProgressRequest request
    ) {
        return SuccessResponse.ok("Progress updated", learningWorkflowService.updateProgress(lessonId, request));
    }

    @PostMapping("/api/v1/lessons/{lessonId}/complete")
    public SuccessResponse<ProgressResponse> completeLesson(@PathVariable String lessonId) {
        return SuccessResponse.ok("Lesson completed", learningWorkflowService.completeLesson(lessonId));
    }
}

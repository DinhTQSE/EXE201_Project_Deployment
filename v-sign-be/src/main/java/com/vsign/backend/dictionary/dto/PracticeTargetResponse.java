package com.vsign.backend.dictionary.dto;

public record PracticeTargetResponse(
        int entryId,
        int lessonId,
        String label,
        boolean requiresPremium,
        String unitId,
        String chapterId,
        String lessonKey,
        String quizId
) {
}

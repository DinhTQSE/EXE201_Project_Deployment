package com.vsign.backend.dictionary.dto;

public record DictionaryEntryResponse(
        int id,
        String entryId,
        String word,
        String keyword,
        String category,
        String difficulty,
        int difficultyLevel,
        String description,
        String videoUrl,
        String thumbnailUrl
) {
}

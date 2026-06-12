package com.vsign.backend.dictionary.service;

import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.dictionary.dto.DictionaryEntryResponse;
import com.vsign.backend.dictionary.dto.PracticeTargetResponse;
import com.vsign.backend.dictionary.persistence.DictionaryEntryEntity;
import com.vsign.backend.dictionary.persistence.DictionaryEntryRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionaryService {
    private final DictionaryEntryRepository dictionaryEntryRepository;

    public DictionaryService(DictionaryEntryRepository dictionaryEntryRepository) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    @Transactional(readOnly = true)
    public DictionaryEntryPage list(String category, String keyword, String difficulty, int page, int size) {
        String normalizedCategory = normalize(category);
        String normalizedKeyword = normalize(keyword);
        Integer difficultyLevel = normalizeDifficulty(difficulty);

        List<DictionaryEntryResponse> filtered = dictionaryEntryRepository.findByPublishedTrue().stream()
                .map(DictionaryService::toResponse)
                .filter(entry -> normalizedCategory == null || entry.category().equalsIgnoreCase(normalizedCategory))
                .filter(entry -> normalizedKeyword == null || entry.word().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .filter(entry -> difficultyLevel == null || entry.difficultyLevel() == difficultyLevel)
                .sorted(Comparator.comparing(DictionaryEntryResponse::id))
                .toList();

        int totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int from = Math.min(page * size, totalElements);
        int to = Math.min(from + size, totalElements);
        List<DictionaryEntryResponse> items = filtered.subList(from, to);
        return new DictionaryEntryPage(items, page, size, totalElements, totalElements, totalPages, items);
    }

    @Transactional(readOnly = true)
    public PracticeTargetResponse practiceTarget(String entryId) {
        Integer parsedEntryId = parseEntryId(entryId);
        DictionaryEntryEntity entry = dictionaryEntryRepository.findById(parsedEntryId)
                .filter(DictionaryEntryEntity::isPublished)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return new PracticeTargetResponse(
                entry.getId(),
                101,
                entry.getWord(),
                false,
                "unit-greetings",
                "chapter-basic-greetings",
                "lesson-hello",
                "quiz-greetings-1"
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private Integer normalizeDifficulty(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "1", "CO_BAN" -> 1;
            case "2", "TRUNG_BINH" -> 2;
            case "3", "NANG_CAO" -> 3;
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        };
    }

    private Integer parseEntryId(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private static DictionaryEntryResponse toResponse(DictionaryEntryEntity entry) {
        return new DictionaryEntryResponse(
                entry.getId(),
                String.valueOf(entry.getId()),
                entry.getWord(),
                entry.getWord(),
                entry.getCategory(),
                entry.getDifficulty(),
                entry.getDifficultyLevel(),
                entry.getDescription(),
                entry.getVideoUrl(),
                entry.getThumbnailUrl()
        );
    }

    public record DictionaryEntryPage(
            List<DictionaryEntryResponse> items,
            int page,
            int size,
            int total,
            int totalElements,
            int totalPages,
            List<DictionaryEntryResponse> content
    ) {
    }
}

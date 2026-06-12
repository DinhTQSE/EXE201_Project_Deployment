package com.vsign.backend.dictionary.controller;

import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.dictionary.dto.PracticeTargetResponse;
import com.vsign.backend.dictionary.service.DictionaryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/dictionary")
public class DictionaryPublicController {
    private final DictionaryService dictionaryService;

    public DictionaryPublicController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public SuccessResponse<DictionaryService.DictionaryEntryPage> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Size(max = 20) String difficulty,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return SuccessResponse.ok("Dictionary entries loaded", dictionaryService.list(category, keyword, difficulty, page, size));
    }

    @GetMapping("/{entryId}/practice-target")
    public SuccessResponse<PracticeTargetResponse> practiceTarget(@PathVariable String entryId) {
        return SuccessResponse.ok("Practice target loaded", dictionaryService.practiceTarget(entryId));
    }
}

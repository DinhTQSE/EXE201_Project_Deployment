package com.vsign.backend.dictionary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DictionaryIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsDefaultPagedEntries() throws Exception {
        mockMvc.perform(get("/api/v1/dictionary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total").value(6))
                .andExpect(jsonPath("$.data.items.length()").value(6));
    }

    @Test
    void appliesCategoryKeywordDifficultyAndPagingFilters() throws Exception {
        mockMvc.perform(get("/api/v1/dictionary")
                        .param("category", "place")
                        .param("keyword", "school")
                        .param("difficulty", "TRUNG_BINH")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(3))
                .andExpect(jsonPath("$.data.items[0].word").value("School"))
                .andExpect(jsonPath("$.data.items[0].category").value("place"))
                .andExpect(jsonPath("$.data.items[0].difficulty").value("TRUNG_BINH"));
    }

    @Test
    void preservesPageMetadataForOutOfRangePage() throws Exception {
        mockMvc.perform(get("/api/v1/dictionary")
                        .param("page", "6")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(6))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.total").value(6))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void rejectsDictionaryQueryLongerThanOneHundredCharacters() throws Exception {
        mockMvc.perform(get("/api/v1/dictionary")
                        .param("keyword", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsInvalidPagingAndDifficultyFilters() throws Exception {
        mockMvc.perform(get("/api/v1/dictionary")
                        .param("difficulty", "KHONG_HOP_LE")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnsPracticeTargetForDictionaryEntry() throws Exception {
        mockMvc.perform(get("/api/v1/dictionary/1/practice-target"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entryId").value(1))
                .andExpect(jsonPath("$.data.lessonId").value(101))
                .andExpect(jsonPath("$.data.label").value("Hello"))
                .andExpect(jsonPath("$.data.requiresPremium").value(false));
    }

    @Test
    void returnsNotFoundWhenPracticeTargetEntryDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/dictionary/missing-entry/practice-target"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}

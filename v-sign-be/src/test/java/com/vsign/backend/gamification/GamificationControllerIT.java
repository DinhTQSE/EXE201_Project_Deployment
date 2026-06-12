package com.vsign.backend.gamification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vsign.backend.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GamificationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void summaryRequiresValidTokenAndUsesTokenIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/gamification/summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/gamification/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer("learner.one@vsign.test", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("learner-001"));
    }

    @Test
    void leaderboardReturnsCurrentUserFromTokenContext() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboards")
                        .param("period", "WEEKLY")
                        .param("page", "0")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer("learner.one@vsign.test", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentUser.userId").value("learner-001"))
                .andExpect(jsonPath("$.data.entries.length()").value(2));
    }

    @Test
    void awardXpIsIdempotentByEventId() throws Exception {
        String requestBody = """
                {
                  "eventId": "lesson-evt-1001",
                  "source": "LESSON_COMPLETE",
                  "xpDelta": 40,
                  "activityDate": "2026-05-22"
                }
                """;

        mockMvc.perform(post("/api/v1/gamification/xp-awards")
                        .header(HttpHeaders.AUTHORIZATION, bearer("learner.one@vsign.test", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(false));

        mockMvc.perform(post("/api/v1/gamification/xp-awards")
                        .header(HttpHeaders.AUTHORIZATION, bearer("learner.one@vsign.test", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(true));
    }

    @Test
    void awardXpRejectsInvalidDelta() throws Exception {
        mockMvc.perform(post("/api/v1/gamification/xp-awards")
                        .header(HttpHeaders.AUTHORIZATION, bearer("learner.one@vsign.test", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "lesson-evt-1002",
                                  "source": "LESSON_COMPLETE",
                                  "xpDelta": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String bearer(String email, String role) {
        return "Bearer " + jwtService.generateToken(email, role);
    }
}

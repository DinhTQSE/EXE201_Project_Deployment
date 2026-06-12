package com.vsign.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void profileReturnsAuthenticatedUser() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "profile-user@vsign.com",
                                  "password": "Secret123",
                                  "fullName": "Profile User"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = body.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("profile-user@vsign.com"))
                .andExpect(jsonPath("$.data.fullName").value("Profile User"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.accountType").value("BASIC"))
                .andExpect(jsonPath("$.data.totalXp").value(0))
                .andExpect(jsonPath("$.data.currentStreak").value(0))
                .andExpect(jsonPath("$.data.longestStreak").value(0))
                .andExpect(jsonPath("$.data.badges").isArray())
                .andExpect(jsonPath("$.data.badges").isEmpty())
                .andExpect(jsonPath("$.data.subscription.planType").value("BASIC"))
                .andExpect(jsonPath("$.data.subscription.status").value("INACTIVE"));
    }

    @Test
    void profileRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void profileUpdatePersistsFullNameAndAvatarUrl() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "profile-update-user@vsign.com",
                                  "password": "Secret123",
                                  "fullName": "Before Update"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = body.path("data").path("accessToken").asText();

        mockMvc.perform(patch("/api/v1/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "After Update",
                                  "avatarUrl": "https://cdn.vsign.test/avatar.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("profile-update-user@vsign.com"))
                .andExpect(jsonPath("$.data.fullName").value("After Update"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.vsign.test/avatar.png"));

        mockMvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("After Update"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.vsign.test/avatar.png"));
    }

    @Test
    void profileUpdateRejectsBlankFullName() throws Exception {
        String accessToken = registerAndReadToken("profile-blank-name@vsign.com");

        mockMvc.perform(patch("/api/v1/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "   ",
                                  "avatarUrl": "https://cdn.vsign.test/avatar.png"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[?(@.field == 'fullName')]").exists());
    }

    @Test
    void profileUpdateRejectsInvalidAvatarUrl() throws Exception {
        String accessToken = registerAndReadToken("profile-invalid-avatar@vsign.com");

        mockMvc.perform(patch("/api/v1/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Avatar User",
                                  "avatarUrl": "not a url"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[?(@.field == 'avatarUrl')]").exists());
    }

    private String registerAndReadToken(String email) throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Secret123",
                                  "fullName": "Profile Validation User"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }
}

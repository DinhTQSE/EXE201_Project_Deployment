package com.vsign.backend.auth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vsign.backend.auth.service.GoogleOAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleOAuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleOAuthService googleOAuthService;

    @Test
    void testGetLoginUrl() throws Exception {
        String mockUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=test-id";
        when(googleOAuthService.getAuthorizationUrl()).thenReturn(mockUrl);

        mockMvc.perform(get("/api/v1/auth/google/login-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(mockUrl));
    }

    @Test
    void testHandleCallbackRedirects() throws Exception {
        String mockRedirect = "http://localhost:5173/oauth-redirect?accessToken=token123";
        when(googleOAuthService.handleCallbackAndGetRedirect(anyString())).thenReturn(mockRedirect);

        mockMvc.perform(get("/api/v1/auth/google/callback").param("code", "xyz123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(mockRedirect));
    }
}

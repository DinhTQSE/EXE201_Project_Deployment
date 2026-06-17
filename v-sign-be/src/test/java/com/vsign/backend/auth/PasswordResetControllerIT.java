package com.vsign.backend.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.common.mail.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @Test
    void testPasswordResetFlow() throws Exception {
        // 1. Register a test user
        String email = "reset-test@vsign.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reset-test@vsign.com",
                                  "password": "SecretPassword123",
                                  "fullName": "Reset Tester"
                                }
                                """))
                .andExpect(status().isCreated());

        // Verify user exists and doesn't have a reset token
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        assertNotNull(user);
        assertNull(user.getPwdResetTokenHash());

        // 2. Request password reset
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reset-test@vsign.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify token hash is generated
        user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        assertNotNull(user);
        String tokenHash = user.getPwdResetTokenHash();
        assertNotNull(tokenHash);
        assertNotNull(user.getPwdResetExpiry());

        // Simulate complete password reset by manually setting token hash
        String testRawToken = "my-custom-test-raw-token-1234567890";
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(testRawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String customHash = hexString.toString();

        user.setPwdResetTokenHash(customHash);
        user.setPwdResetExpiry(java.time.OffsetDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // 3. Complete password reset with wrong confirmPassword
        mockMvc.perform(post("/api/v1/auth/password-reset/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "token": "%s",
                                  "newPassword": "NewSecretPassword123",
                                  "confirmPassword": "DifferentPassword123"
                                }
                                """, testRawToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // 4. Complete password reset with weak password
        mockMvc.perform(post("/api/v1/auth/password-reset/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "token": "%s",
                                  "newPassword": "weak",
                                  "confirmPassword": "weak"
                                }
                                """, testRawToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // 5. Complete password reset successfully
        mockMvc.perform(post("/api/v1/auth/password-reset/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "token": "%s",
                                  "newPassword": "NewSecretPassword123",
                                  "confirmPassword": "NewSecretPassword123"
                                }
                                """, testRawToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify password hash changed and reset fields are cleared
        user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        assertNotNull(user);
        assertNull(user.getPwdResetTokenHash());
        assertNull(user.getPwdResetExpiry());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("NewSecretPassword123", user.getPasswordHash()));
    }
}

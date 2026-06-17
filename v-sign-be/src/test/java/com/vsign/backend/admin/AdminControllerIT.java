package com.vsign.backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class AdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void blocksNonAdminFromAdminUsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer("learner.one@vsign.test", "USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listsUsersForAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("role", "USER")
                        .param("status", "ACTIVE")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.users.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.users[0].email").exists());
    }

    @Test
    void loadsAndUpdatesRealUserWithAuditLog() throws Exception {
        String learnerId = "00000000-0000-0000-0000-000000000101";

        mockMvc.perform(get("/api/v1/admin/users/{userId}", learnerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("learner.basic@vsign.test"))
                .andExpect(jsonPath("$.data.activity.activeSeconds").value(900));

        mockMvc.perform(patch("/api/v1/admin/users/{userId}", learnerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Learner Basic Updated",
                                  "accountType": "PREMIUM",
                                  "reason": "Support account correction"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.displayName").value("Learner Basic Updated"))
                .andExpect(jsonPath("$.data.user.accountType").value("PREMIUM"));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.action == 'USER_UPDATED')]").exists());
    }

    @Test
    void blocksAdminRoleChangeAndSelfDisable() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/00000000-0000-0000-0000-000000000101")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN",
                                  "reason": "Role escalation attempt"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/v1/admin/users/00000000-0000-0000-0000-000000000901")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "active": false,
                                  "reason": "Self disable attempt"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsReviewerQueueReadAndAdminDecisionWrite() throws Exception {
        mockMvc.perform(get("/api/v1/admin/content/review-queue")
                        .header(HttpHeaders.AUTHORIZATION, bearer("reviewer@vsign.test", "CONTENT_REVIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3));

        mockMvc.perform(patch("/api/v1/admin/content/review-queue/doc-2001")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "reason": "Quality standards met"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void requiresReasonForManualPaymentOverride() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/payments/txn-1001")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PAID",
                                  "reason": "ok"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void writesAuditLogWhenPaymentOverrideSucceeds() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/payments/txn-1001")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PAID",
                                  "reason": "Gateway confirmation received"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[?(@.action == 'PAYMENT_STATUS_OVERRIDE')]").exists());
    }

    @Test
    void returnsAdminKpisForDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/admin/kpis")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successfulTransactions").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalRevenueVnd").value(org.hamcrest.Matchers.greaterThanOrEqualTo(199000)));
    }

    @Test
    void returnsAdminMetricsOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/overview")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@vsign.test", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(6))
                .andExpect(jsonPath("$.data.activeUsers").value(5))
                .andExpect(jsonPath("$.data.premiumUsers").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.activeUsersInRange").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.averageActiveSeconds").value(org.hamcrest.Matchers.greaterThanOrEqualTo(600)));
    }

    private String bearer(String email, String role) {
        return "Bearer " + jwtService.generateToken(email, role);
    }
}

package com.vsign.backend.monetization;

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
class SubscriptionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void listsSubscriptionPlansWithConcreteDtoFields() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].planId").value("free"));
    }

    @Test
    void listsActivePlansFromPlannedSingularEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/subscription/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].planType").value("MONTHLY"))
                .andExpect(jsonPath("$.data[0].price").value(49000))
                .andExpect(jsonPath("$.data[0].currency").value("VND"));
    }

    @Test
    void createsCheckoutIntentForPaidPlan() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions/checkout")
                        .header(HttpHeaders.AUTHORIZATION, bearer("checkout-user@vsign.vn", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": "pro-monthly",
                                  "userId": "user-1001",
                                  "successUrl": "https://vsign.test/payment/success",
                                  "cancelUrl": "https://vsign.test/payment/cancel"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planId").value("pro-monthly"));
    }

    @Test
    void createsPaymentOrderForMomoPlan() throws Exception {
        mockMvc.perform(post("/api/v1/payments/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer("payment-user@vsign.vn", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "MOMO",
                                  "planType": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.provider").value("MOMO"))
                .andExpect(jsonPath("$.data.planType").value("MONTHLY"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void rejectsInvalidPaymentProviderAmountAndPlan() throws Exception {
        mockMvc.perform(post("/api/v1/payments/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer("payment-user@vsign.vn", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "CARD",
                                  "planType": "MONTHLY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/payments/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer("payment-user@vsign.vn", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "MOMO",
                                  "planId": "pro-monthly",
                                  "amount": 1000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/payments/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer("payment-user@vsign.vn", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "ZALOPAY",
                                  "planId": "retired-plan",
                                  "amount": 99000
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void returnsPaymentStatusAndMissingTransactionError() throws Exception {
        String response = mockMvc.perform(post("/api/v1/payments/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer("payment-user@vsign.vn", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "MOMO",
                                  "planType": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String txId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).path("data").path("transactionId").asText();

        mockMvc.perform(get("/api/v1/payments/{transactionId}", txId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/payments/{transactionId}", txId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("payment-user@vsign.vn", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value(txId))
                .andExpect(jsonPath("$.data.status").isNotEmpty())
                .andExpect(jsonPath("$.data.retryable").isBoolean());

        mockMvc.perform(get("/api/v1/payments/{transactionId}", txId)
                        .header(HttpHeaders.AUTHORIZATION, bearer("other-payment-user@vsign.vn", "USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/v1/payments/missing-transaction")
                        .header(HttpHeaders.AUTHORIZATION, bearer("payment-user@vsign.vn", "USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void returnsCurrentSubscriptionAndPaymentHistoryForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/v1/payments/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer("payment-user@vsign.vn", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "ZALOPAY",
                                  "planType": "YEARLY"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/subscription")
                        .header(HttpHeaders.AUTHORIZATION, bearer("fresh-subscription@vsign.vn", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FREE"))
                .andExpect(jsonPath("$.data.planType").doesNotExist());

        mockMvc.perform(get("/api/v1/me/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer("payment-user@vsign.vn", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data[0].planType").value("YEARLY"));

        mockMvc.perform(get("/api/v1/me/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer("fresh-subscription@vsign.vn", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private String bearer(String email, String role) {
        return "Bearer " + jwtService.generateToken(email, role);
    }
}

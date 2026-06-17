package com.vsign.backend.payment.controller;

import com.vsign.backend.common.security.JwtAuthFilter;
import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.common.security.SecurityConfig;
import com.vsign.backend.payment.service.PayOSPaymentService;
import com.vsign.backend.payment.service.PayOSWebhookService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import vn.payos.PayOS;
import vn.payos.service.blocking.webhooks.WebhooksService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PayOSWebhookController.class, PayOSPaymentController.class})
@Import(SecurityConfig.class)
class PayOSWebhookControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PayOS payOS;

    @MockBean
    PayOSWebhookService webhookService;

    @MockBean
    PayOSPaymentService paymentService;

    @MockBean
    JwtAuthFilter jwtAuthFilter;

    @MockBean
    JwtService jwtService;

    @MockBean
    WebhooksService webhooksService;

    @BeforeEach
    void setUp() throws Exception {
        // Make the mock filter pass-through so the security filter chain continues
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        when(payOS.webhooks()).thenReturn(webhooksService);
    }

    @Test
    void webhookGetReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/payments/payos/webhook"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void webhookPostRejectsInvalidSignature() throws Exception {
        when(webhooksService.verify(any())).thenThrow(new RuntimeException("bad signature"));

        mockMvc.perform(post("/api/v1/payments/payos/webhook")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid webhook"));
    }

    @Test
    void tiersEndpointIsPublic() throws Exception {
        when(paymentService.listActiveTiers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/payments/tiers"))
                .andExpect(status().isOk());
    }

    @Test
    void checkoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/payments/checkout")
                        .contentType("application/json")
                        .content("{\"tierId\":\"00000000-0000-0000-0000-000000000002\"}"))
                .andExpect(status().isUnauthorized());
    }
}

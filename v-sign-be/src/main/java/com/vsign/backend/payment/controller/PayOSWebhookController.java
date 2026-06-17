package com.vsign.backend.payment.controller;

import com.vsign.backend.payment.service.PayOSWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.model.webhooks.Webhook;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/payos/webhook")
@RequiredArgsConstructor
public class PayOSWebhookController {

    private final PayOS payOS;
    private final PayOSWebhookService webhookService;

    @GetMapping
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping
    public ResponseEntity<String> webhook(@RequestBody Webhook webhook) {
        var data = verifyWebhook(webhook);
        if (data == null) {
            return ResponseEntity.badRequest().body("Invalid webhook signature");
        }
        try {
            webhookService.handlePayOSWebhook(data);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("PayOS webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Webhook processing error");
        }
    }

    private vn.payos.model.webhooks.WebhookData verifyWebhook(Webhook webhook) {
        try {
            return payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            log.warn("PayOS webhook signature verification failed: {}", e.getMessage());
            return null;
        }
    }
}

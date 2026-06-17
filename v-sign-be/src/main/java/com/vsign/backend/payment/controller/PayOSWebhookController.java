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
        try {
            var data = payOS.webhooks().verify(webhook);
            webhookService.handlePayOSWebhook(data);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.warn("PayOS webhook rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid webhook");
        }
    }
}

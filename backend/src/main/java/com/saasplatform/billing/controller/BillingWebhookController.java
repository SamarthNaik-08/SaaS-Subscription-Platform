package com.saasplatform.billing.controller;

import com.saasplatform.billing.service.BillingWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/webhook")
@RequiredArgsConstructor
public class BillingWebhookController {

    private final BillingWebhookService billingWebhookService;

    @PostMapping("/razorpay")
    public ResponseEntity<Map<String, String>> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader
    ) {
        log.info("Received incoming payment webhook notification");
        billingWebhookService.processRazorpayWebhook(rawPayload, signatureHeader);
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Webhook received and processed"));
    }
}

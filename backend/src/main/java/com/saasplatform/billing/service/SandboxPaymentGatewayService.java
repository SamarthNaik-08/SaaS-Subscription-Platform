package com.saasplatform.billing.service;

import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.common.enums.PaymentGatewayProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Slf4j
@Service("sandboxPaymentGatewayService")
public class SandboxPaymentGatewayService implements PaymentGatewayService {

    public static final String SANDBOX_SECRET = "sandbox_secret_key_saas_platform_2026";
    public static final String SANDBOX_WEBHOOK_SECRET = "sandbox_webhook_secret_saas_platform_2026";
    public static final String SANDBOX_KEY_ID = "rzp_sandbox_mockKeyId";

    @Override
    public String createGatewayOrder(PaymentOrder internalOrder) {
        String orderId = "order_sandbox_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Created Sandbox gateway order={} for internal order={}", orderId, internalOrder.getId());
        return orderId;
    }

    @Override
    public boolean verifyPaymentSignature(String gatewayOrderId, String gatewayPaymentId, String gatewaySignature) {
        if (gatewayOrderId == null || gatewayPaymentId == null || gatewaySignature == null) {
            return false;
        }
        try {
            String payload = gatewayOrderId + "|" + gatewayPaymentId;
            String expectedSignature = RazorpayPaymentGatewayService.calculateHmacSha256(payload, SANDBOX_SECRET);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    gatewaySignature.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Sandbox signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (rawBody == null || signatureHeader == null) {
            return false;
        }
        try {
            String expectedSignature = RazorpayPaymentGatewayService.calculateHmacSha256(rawBody, SANDBOX_WEBHOOK_SECRET);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Sandbox webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public PaymentGatewayProvider getProvider() {
        return PaymentGatewayProvider.SANDBOX;
    }

    @Override
    public String getPublicKeyId() {
        return SANDBOX_KEY_ID;
    }
}

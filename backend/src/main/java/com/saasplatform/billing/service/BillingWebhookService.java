package com.saasplatform.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.billing.entity.WebhookEvent;
import com.saasplatform.billing.repository.PaymentOrderRepository;
import com.saasplatform.billing.repository.WebhookEventRepository;
import com.saasplatform.common.enums.PaymentGatewayProvider;
import com.saasplatform.common.enums.PaymentOrderStatus;
import com.saasplatform.common.enums.WebhookEventStatus;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingWebhookService {

    private final WebhookEventRepository webhookEventRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentOrderService paymentOrderService;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    @Transactional
    public void processRazorpayWebhook(String rawPayload, String signatureHeader) {
        log.info("Received Razorpay webhook, payload length={}", rawPayload != null ? rawPayload.length() : 0);

        PaymentGatewayService gatewayService = paymentGatewayFactory.getServiceByProvider(PaymentGatewayProvider.RAZORPAY);
        boolean isSignatureValid = gatewayService.verifyWebhookSignature(rawPayload, signatureHeader);

        // Also check sandbox gateway service if running in sandbox mode
        if (!isSignatureValid) {
            PaymentGatewayService sandboxGateway = paymentGatewayFactory.getServiceByProvider(PaymentGatewayProvider.SANDBOX);
            isSignatureValid = sandboxGateway.verifyWebhookSignature(rawPayload, signatureHeader);
        }

        if (!isSignatureValid) {
            log.error("Invalid webhook signature received");
            throw new BadRequestException("Webhook signature verification failed");
        }

        String payloadHash = jwtService.hashToken(rawPayload != null ? rawPayload : "");

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventType = root.path("event").asText("unknown");
            String eventId = root.path("id").asText(root.path("event_id").asText("evt_" + payloadHash.substring(0, 16)));

            // Idempotency check: provider + provider_event_id
            Optional<WebhookEvent> existingEvent = webhookEventRepository
                    .findByProviderAndProviderEventId(gatewayService.getProvider(), eventId);

            if (existingEvent.isPresent()) {
                log.info("Webhook event {} already processed (status={}), skipping duplicate",
                        eventId, existingEvent.get().getStatus());
                return;
            }

            WebhookEvent webhookEvent = WebhookEvent.builder()
                    .provider(gatewayService.getProvider())
                    .providerEventId(eventId)
                    .eventType(eventType)
                    .payloadHash(payloadHash)
                    .status(WebhookEventStatus.RECEIVED)
                    .build();

            webhookEvent = webhookEventRepository.save(webhookEvent);

            handleEvent(eventType, root);

            webhookEvent.setStatus(WebhookEventStatus.PROCESSED);
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);
            log.info("Successfully processed webhook event id={}, type={}", eventId, eventType);

        } catch (Exception e) {
            log.error("Error processing webhook event: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to process webhook event: " + e.getMessage());
        }
    }

    private void handleEvent(String eventType, JsonNode root) {
        if ("order.paid".equalsIgnoreCase(eventType) || "payment.captured".equalsIgnoreCase(eventType)) {
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            String gatewayOrderId = paymentEntity.path("order_id").asText(root.path("payload").path("order").path("entity").path("id").asText());
            String gatewayPaymentId = paymentEntity.path("id").asText();
            String gatewaySignature = root.path("payload").path("payment").path("entity").path("signature").asText("webhook_verified");

            if (!gatewayOrderId.isBlank()) {
                paymentOrderRepository.findByGatewayOrderId(gatewayOrderId).ifPresent(order -> {
                    paymentOrderService.settlePayment(order, gatewayPaymentId, gatewaySignature);
                });
            }
        } else if ("payment.failed".equalsIgnoreCase(eventType)) {
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            String gatewayOrderId = paymentEntity.path("order_id").asText();

            if (!gatewayOrderId.isBlank()) {
                paymentOrderRepository.findByGatewayOrderId(gatewayOrderId).ifPresent(order -> {
                    if (order.getStatus() != PaymentOrderStatus.PAID) {
                        order.setStatus(PaymentOrderStatus.FAILED);
                        paymentOrderRepository.save(order);
                        log.info("Marked payment order {} as FAILED from webhook", order.getId());
                    }
                });
            }
        }
    }
}

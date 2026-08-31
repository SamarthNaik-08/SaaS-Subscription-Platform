package com.saasplatform.billing.entity;

import com.saasplatform.common.enums.PaymentGatewayProvider;
import com.saasplatform.common.enums.WebhookEventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_webhook_events_provider_event", columnNames = {"provider", "provider_event_id"})
}, indexes = {
        @Index(name = "idx_webhook_events_provider_event", columnList = "provider, provider_event_id", unique = true),
        @Index(name = "idx_webhook_events_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentGatewayProvider provider;

    @Column(name = "provider_event_id", nullable = false, length = 150)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private WebhookEventStatus status = WebhookEventStatus.RECEIVED;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}

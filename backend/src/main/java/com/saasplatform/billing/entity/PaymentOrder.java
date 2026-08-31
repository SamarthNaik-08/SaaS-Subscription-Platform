package com.saasplatform.billing.entity;

import com.saasplatform.common.enums.BillingInterval;
import com.saasplatform.common.enums.PaymentGatewayProvider;
import com.saasplatform.common.enums.PaymentOrderStatus;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_orders", indexes = {
        @Index(name = "idx_payment_orders_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_orders_gateway_order", columnList = "gateway_order_id", unique = true),
        @Index(name = "idx_payment_orders_gateway_payment", columnList = "gateway_payment_id"),
        @Index(name = "idx_payment_orders_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 20)
    private BillingInterval billingInterval;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentOrderStatus status = PaymentOrderStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_provider", nullable = false, length = 30)
    private PaymentGatewayProvider gatewayProvider;

    @Column(name = "gateway_order_id", nullable = false, unique = true, length = 100)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Column(name = "gateway_signature", length = 255)
    private String gatewaySignature;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}

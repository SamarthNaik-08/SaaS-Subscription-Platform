package com.saasplatform.billing.dto;

import com.saasplatform.common.enums.PaymentOrderStatus;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.common.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationResponse {

    private UUID orderId;
    private PaymentOrderStatus orderStatus;
    private PlanCode planCode;
    private String planName;
    private SubscriptionStatus subscriptionStatus;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private String message;
}

package com.saasplatform.billing.dto;

import com.saasplatform.common.enums.BillingInterval;
import com.saasplatform.common.enums.PaymentGatewayProvider;
import com.saasplatform.common.enums.PaymentOrderStatus;
import com.saasplatform.common.enums.PlanCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderDto {

    private UUID id;
    private PlanCode planCode;
    private String planName;
    private BillingInterval billingInterval;
    private BigDecimal amount;
    private String currency;
    private PaymentOrderStatus status;
    private PaymentGatewayProvider gatewayProvider;
    private String gatewayOrderId;
    private String keyId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}

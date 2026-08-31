package com.saasplatform.subscription.dto;

import com.saasplatform.common.enums.SubscriptionStatus;
import com.saasplatform.plan.dto.PlanDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    private UUID id;
    private UUID userId;
    private PlanDto plan;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private boolean cancelAtPeriodEnd;
    private LocalDateTime cancelledAt;
    private String paymentProvider;
    private String externalSubscriptionId;
}

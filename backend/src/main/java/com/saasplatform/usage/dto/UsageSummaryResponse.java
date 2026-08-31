package com.saasplatform.usage.dto;

import com.saasplatform.common.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageSummaryResponse {
    private String userEmail;
    private String planName;
    private String planCode;
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Map<String, MetricUsageDto> metrics;
}

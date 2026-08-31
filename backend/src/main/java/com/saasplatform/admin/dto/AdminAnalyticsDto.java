package com.saasplatform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsDto {
    private BigDecimal mrr;
    private BigDecimal arr;
    private BigDecimal totalRevenue;
    private long totalUsers;
    private long activeUsers;
    private long activePaidSubscribers;
    private long freeUsers;
    private long proUsers;
    private long businessUsers;
    private double conversionRate;
    private double churnRate;
    private BigDecimal arppu;
    private long totalAiUsage;
    private long paymentSuccessCount;
    private long paymentFailureCount;
    private double paymentSuccessRate;
    private Map<String, BigDecimal> revenueByPlan;
    private Map<String, Long> usageByPlan;
}

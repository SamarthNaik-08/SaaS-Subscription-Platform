package com.saasplatform.admin.dto;

import com.saasplatform.audit.entity.AuditLog;
import com.saasplatform.billing.dto.PaymentOrderDto;
import com.saasplatform.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {
    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long totalSubscriptions;
    private long activePaidSubscriptions;
    private Map<String, Long> subscriptionsByPlan;
    private BigDecimal mrr;
    private BigDecimal arr;
    private BigDecimal totalRevenue;
    private long paymentSuccessCount;
    private long paymentFailureCount;
    private double paymentSuccessRate;
    private long totalAiRequests;
    private List<UserDto> recentUsers;
    private List<PaymentOrderDto> recentPayments;
    private List<AuditLog> recentAuditLogs;
}

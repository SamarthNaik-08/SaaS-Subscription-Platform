package com.saasplatform.admin.dto;

import com.saasplatform.audit.entity.AuditLog;
import com.saasplatform.billing.dto.InvoiceDto;
import com.saasplatform.billing.dto.PaymentOrderDto;
import com.saasplatform.subscription.dto.SubscriptionDto;
import com.saasplatform.usage.dto.MetricUsageDto;
import com.saasplatform.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailDto {
    private UserDto user;
    private SubscriptionDto subscription;
    private Map<String, MetricUsageDto> currentUsage;
    private List<PaymentOrderDto> paymentOrders;
    private List<InvoiceDto> invoices;
    private List<AuditLog> auditLogs;
}

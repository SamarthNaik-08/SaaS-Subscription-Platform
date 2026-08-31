package com.saasplatform.admin.service;

import com.saasplatform.admin.dto.*;
import com.saasplatform.audit.entity.AuditAction;
import com.saasplatform.audit.entity.AuditLog;
import com.saasplatform.audit.repository.AuditLogRepository;
import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.auth.service.AuthService;
import com.saasplatform.billing.dto.InvoiceDto;
import com.saasplatform.billing.dto.PaymentOrderDto;
import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.billing.repository.InvoiceRepository;
import com.saasplatform.billing.repository.PaymentOrderRepository;
import com.saasplatform.billing.service.InvoiceService;
import com.saasplatform.billing.service.PaymentGatewayFactory;
import com.saasplatform.common.enums.UserStatus;
import com.saasplatform.exception.ResourceNotFoundException;
import com.saasplatform.plan.dto.PlanDto;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.plan.repository.PlanRepository;
import com.saasplatform.subscription.dto.SubscriptionDto;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.repository.SubscriptionRepository;
import com.saasplatform.subscription.service.SubscriptionService;
import com.saasplatform.usage.dto.CurrentUsageResponse;
import com.saasplatform.usage.service.UsageService;
import com.saasplatform.user.dto.UserDto;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final AdminAnalyticsService adminAnalyticsService;
    private final AuthService authService;
    private final SubscriptionService subscriptionService;
    private final UsageService usageService;
    private final InvoiceService invoiceService;
    private final PaymentGatewayFactory paymentGatewayFactory;

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard() {
        AdminAnalyticsDto analytics = adminAnalyticsService.calculateAnalytics();

        List<UserDto> recentUsers = userRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(authService::mapToUserDto)
                .collect(Collectors.toList());

        String keyId = paymentGatewayFactory.getPaymentGatewayService().getPublicKeyId();
        List<PaymentOrderDto> recentPayments = paymentOrderRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(order -> mapToPaymentDto(order, keyId))
                .collect(Collectors.toList());

        List<AuditLog> recentAuditLogs = auditLogRepository.findTop20ByOrderByCreatedAtDesc();

        Map<String, Long> subsByPlan = new HashMap<>();
        subsByPlan.put("FREE", analytics.getFreeUsers());
        subsByPlan.put("PRO", analytics.getProUsers());
        subsByPlan.put("BUSINESS", analytics.getBusinessUsers());

        return AdminDashboardDto.builder()
                .totalUsers(analytics.getTotalUsers())
                .activeUsers(analytics.getActiveUsers())
                .suspendedUsers(analytics.getTotalUsers() - analytics.getActiveUsers())
                .totalSubscriptions(analytics.getFreeUsers() + analytics.getActivePaidSubscribers())
                .activePaidSubscriptions(analytics.getActivePaidSubscribers())
                .subscriptionsByPlan(subsByPlan)
                .mrr(analytics.getMrr())
                .arr(analytics.getArr())
                .totalRevenue(analytics.getTotalRevenue())
                .paymentSuccessCount(analytics.getPaymentSuccessCount())
                .paymentFailureCount(analytics.getPaymentFailureCount())
                .paymentSuccessRate(analytics.getPaymentSuccessRate())
                .totalAiRequests(analytics.getTotalAiUsage())
                .recentUsers(recentUsers)
                .recentPayments(recentPayments)
                .recentAuditLogs(recentAuditLogs)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsers(String search, Pageable pageable) {
        Page<User> users;
        if (search != null && !search.isBlank()) {
            users = userRepository.findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                    search.trim(), search.trim(), search.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(authService::mapToUserDto);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).orElse(null);
        SubscriptionDto subDto = subscription != null ? subscriptionService.mapToSubscriptionDto(subscription) : null;

        CurrentUsageResponse usageResponse = subscription != null ? usageService.getCurrentUsage(userId) : null;

        String keyId = paymentGatewayFactory.getPaymentGatewayService().getPublicKeyId();
        List<PaymentOrderDto> paymentOrders = paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(p -> mapToPaymentDto(p, keyId))
                .collect(Collectors.toList());

        List<InvoiceDto> invoices = invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(invoiceService::mapToDto)
                .collect(Collectors.toList());

        List<AuditLog> auditLogs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return AdminUserDetailDto.builder()
                .user(authService.mapToUserDto(user))
                .subscription(subDto)
                .currentUsage(usageResponse != null ? usageResponse.getMetrics() : Map.of())
                .paymentOrders(paymentOrders)
                .invoices(invoices)
                .auditLogs(auditLogs)
                .build();
    }

    @Transactional
    public UserDto updateUserStatus(UUID adminUserId, String adminEmail, UUID targetUserId, UserStatus newStatus) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        user = userRepository.save(user);

        auditLogService.logEvent(
                adminUserId,
                adminEmail,
                AuditAction.ADMIN_USER_STATUS_CHANGED,
                "User",
                user.getId().toString(),
                String.format("Updated status of user %s from %s to %s", user.getEmail(), oldStatus, newStatus),
                null
        );

        log.info("Admin {} changed status of user {} to {}", adminEmail, user.getEmail(), newStatus);
        return authService.mapToUserDto(user);
    }

    @Transactional(readOnly = true)
    public List<PlanDto> getAllPlans() {
        return planRepository.findAll().stream()
                .map(PlanDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PlanDto updatePlan(UUID adminUserId, String adminEmail, UUID planId, UpdatePlanRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));

        String oldDetails = String.format("name=%s, monthly=%s, yearly=%s, limit=%d",
                plan.getName(), plan.getPriceMonthly(), plan.getPriceYearly(), plan.getMonthlyAiLimit());

        plan.setName(request.getName().trim());
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription().trim());
        }
        plan.setPriceMonthly(request.getPriceMonthly());
        plan.setPriceYearly(request.getPriceYearly());
        plan.setMonthlyAiLimit(request.getMonthlyAiLimit());
        plan.setStorageLimitMb(request.getStorageLimitMb());
        if (request.getIsActive() != null) {
            plan.setActive(request.getIsActive());
        }

        plan = planRepository.save(plan);

        String newDetails = String.format("name=%s, monthly=%s, yearly=%s, limit=%d",
                plan.getName(), plan.getPriceMonthly(), plan.getPriceYearly(), plan.getMonthlyAiLimit());

        auditLogService.logEvent(
                adminUserId,
                adminEmail,
                AuditAction.ADMIN_PLAN_UPDATED,
                "Plan",
                plan.getId().toString(),
                String.format("Updated plan %s: [Old: %s] -> [New: %s]", plan.getCode(), oldDetails, newDetails),
                null
        );

        log.info("Admin {} updated plan {}", adminEmail, plan.getCode());
        return PlanDto.fromEntity(plan);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionDto> getSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable)
                .map(subscriptionService::mapToSubscriptionDto);
    }

    @Transactional(readOnly = true)
    public Page<PaymentOrderDto> getPayments(Pageable pageable) {
        String keyId = paymentGatewayFactory.getPaymentGatewayService().getPublicKeyId();
        return paymentOrderRepository.findAll(pageable)
                .map(order -> mapToPaymentDto(order, keyId));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceDto> getInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable)
                .map(invoiceService::mapToDto);
    }

    @Transactional(readOnly = true)
    public AdminHealthDto getHealth() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        boolean dbConnected = false;
        try {
            userRepository.count();
            dbConnected = true;
        } catch (Exception e) {
            log.error("Database health check failed", e);
        }

        return AdminHealthDto.builder()
                .status(dbConnected ? "UP" : "DOWN")
                .timestamp(LocalDateTime.now())
                .uptimeSeconds(uptimeSeconds)
                .javaVersion(System.getProperty("java.version"))
                .availableProcessors(runtime.availableProcessors())
                .totalMemoryBytes(runtime.totalMemory())
                .freeMemoryBytes(runtime.freeMemory())
                .maxMemoryBytes(runtime.maxMemory())
                .activeThreads(threadBean.getThreadCount())
                .databaseConnected(dbConnected)
                .details(Map.of(
                        "heapMemoryUsedBytes", memoryBean.getHeapMemoryUsage().getUsed(),
                        "heapMemoryMaxBytes", memoryBean.getHeapMemoryUsage().getMax()
                ))
                .build();
    }

    private PaymentOrderDto mapToPaymentDto(PaymentOrder order, String keyId) {
        return PaymentOrderDto.builder()
                .id(order.getId())
                .planCode(order.getPlan() != null ? order.getPlan().getCode() : null)
                .planName(order.getPlan() != null ? order.getPlan().getName() : "SaaS Plan")
                .billingInterval(order.getBillingInterval())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .gatewayProvider(order.getGatewayProvider())
                .gatewayOrderId(order.getGatewayOrderId())
                .keyId(keyId)
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .build();
    }
}

package com.saasplatform.admin.controller;

import com.saasplatform.admin.dto.*;
import com.saasplatform.admin.service.AdminAnalyticsService;
import com.saasplatform.admin.service.AdminService;
import com.saasplatform.audit.entity.AuditAction;
import com.saasplatform.audit.entity.AuditLog;
import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.billing.dto.InvoiceDto;
import com.saasplatform.billing.dto.PaymentOrderDto;
import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.plan.dto.PlanDto;
import com.saasplatform.security.UserPrincipal;
import com.saasplatform.subscription.dto.SubscriptionDto;
import com.saasplatform.user.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AdminAnalyticsService adminAnalyticsService;
    private final AuditLogService auditLogService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> getDashboard() {
        AdminDashboardDto dashboard = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Admin dashboard metrics retrieved successfully"));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AdminAnalyticsDto>> getAnalytics() {
        AdminAnalyticsDto analytics = adminAnalyticsService.calculateAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics, "Authoritative SaaS analytics calculated successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserDto> users = adminService.getUsers(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "Platform users retrieved successfully"));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> getUserDetail(@PathVariable UUID id) {
        AdminUserDetailDto detail = adminService.getUserDetail(id);
        return ResponseEntity.ok(ApiResponse.success(detail, "User details retrieved successfully"));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal
    ) {
        UserDto updated = adminService.updateUserStatus(adminPrincipal.getId(), adminPrincipal.getEmail(), id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(updated, "User status updated successfully"));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanDto>>> getAllPlans() {
        List<PlanDto> plans = adminService.getAllPlans();
        return ResponseEntity.ok(ApiResponse.success(plans, "All platform plans retrieved"));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> updatePlan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlanRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal
    ) {
        PlanDto updated = adminService.updatePlan(adminPrincipal.getId(), adminPrincipal.getEmail(), id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Plan configuration updated successfully"));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<Page<SubscriptionDto>>> getSubscriptions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<SubscriptionDto> subscriptions = adminService.getSubscriptions(pageable);
        return ResponseEntity.ok(ApiResponse.success(subscriptions, "Platform subscriptions retrieved"));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<Page<PaymentOrderDto>>> getPayments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentOrderDto> payments = adminService.getPayments(pageable);
        return ResponseEntity.ok(ApiResponse.success(payments, "Platform payments retrieved"));
    }

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<Page<InvoiceDto>>> getInvoices(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<InvoiceDto> invoices = adminService.getInvoices(pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices, "Platform invoices retrieved"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) UUID userId,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditLog> logs;
        if (action != null) {
            logs = auditLogService.getAuditLogsByAction(action, pageable);
        } else if (userId != null) {
            logs = auditLogService.getAuditLogsByUser(userId, pageable);
        } else {
            logs = auditLogService.getAuditLogs(pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(logs, "Platform audit logs retrieved"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<AdminHealthDto>> getHealth() {
        AdminHealthDto health = adminService.getHealth();
        return ResponseEntity.ok(ApiResponse.success(health, "Platform operational health retrieved"));
    }
}

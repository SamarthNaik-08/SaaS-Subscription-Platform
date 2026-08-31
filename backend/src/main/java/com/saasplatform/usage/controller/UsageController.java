package com.saasplatform.usage.controller;

import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.security.UserPrincipal;
import com.saasplatform.usage.dto.*;
import com.saasplatform.usage.service.UsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<CurrentUsageResponse>> getCurrentUsage(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CurrentUsageResponse usage = usageService.getCurrentUsage(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(usage, "Current usage metrics retrieved successfully"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<UsageHistoryResponse>> getUsageHistory(
            @RequestParam(required = false) UsageMetric metric,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UsageHistoryResponse history = usageService.getUsageHistory(userPrincipal.getId(), metric);
        return ResponseEntity.ok(ApiResponse.success(history, "Usage history retrieved successfully"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<UsageSummaryResponse>> getUsageSummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UsageSummaryResponse summary = usageService.getUsageSummary(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(summary, "Usage summary retrieved successfully"));
    }

    /**
     * Development / Sandbox simulation endpoint for testing real-time UI meters and 429 quota exhaustion.
     * Note: Authoritative production usage is triggered internally by trusted backend services via UsageService.consume(...).
     */
    @PostMapping({"/simulate", "/record"})
    public ResponseEntity<ApiResponse<MetricUsageDto>> simulateUsage(
            @Valid @RequestBody RecordUsageRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (request.getQuantity() > 10) {
            throw new com.saasplatform.exception.BadRequestException("Client test simulations are limited to a maximum of 10 units per request");
        }

        MetricUsageDto result = usageService.recordUsage(
                userPrincipal.getId(),
                request.getMetric(),
                request.getQuantity(),
                request.getMetadata() != null ? "[Simulated] " + request.getMetadata() : "[Simulated Test Inference]"
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Usage simulated successfully"));
    }
}

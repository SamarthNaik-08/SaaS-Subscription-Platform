package com.saasplatform.billing.controller;

import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.security.UserPrincipal;
import com.saasplatform.subscription.dto.SubscriptionDto;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/subscription")
@RequiredArgsConstructor
public class SubscriptionBillingController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SubscriptionDto>> getCurrentSubscription(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        SubscriptionDto subscription = subscriptionService.getCurrentSubscriptionDto(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(subscription, "Current subscription retrieved"));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelSubscription(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Subscription subscription = subscriptionService.cancelSubscription(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(null,
                "Subscription scheduled for cancellation at period end (" + subscription.getCurrentPeriodEnd() + ")"));
    }

    @PostMapping("/resume")
    public ResponseEntity<ApiResponse<Void>> resumeSubscription(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Subscription subscription = subscriptionService.resumeSubscription(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(null,
                "Subscription renewed and active until " + subscription.getCurrentPeriodEnd()));
    }
}

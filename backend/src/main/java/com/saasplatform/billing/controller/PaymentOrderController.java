package com.saasplatform.billing.controller;

import com.saasplatform.billing.dto.*;
import com.saasplatform.billing.service.PaymentOrderService;
import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    @PostMapping("/orders/create")
    public ResponseEntity<ApiResponse<PaymentOrderDto>> createPaymentOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        PaymentOrderDto order = paymentOrderService.createPaymentOrder(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Payment order created successfully"));
    }

    @PostMapping("/orders/verify")
    public ResponseEntity<ApiResponse<PaymentVerificationResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        PaymentVerificationResponse response = paymentOrderService.verifyPayment(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<PaymentOrderDto>>> getPaymentOrders(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<PaymentOrderDto> orders = paymentOrderService.getPaymentOrders(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(orders, "Payment orders retrieved successfully"));
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<BillingConfigResponse>> getBillingConfig() {
        BillingConfigResponse config = paymentOrderService.getBillingConfig();
        return ResponseEntity.ok(ApiResponse.success(config, "Billing configuration retrieved successfully"));
    }
}

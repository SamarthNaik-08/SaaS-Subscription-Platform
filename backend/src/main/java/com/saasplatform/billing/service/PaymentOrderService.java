package com.saasplatform.billing.service;

import com.saasplatform.audit.entity.AuditAction;
import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.billing.dto.*;
import com.saasplatform.billing.entity.Invoice;
import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.billing.repository.PaymentOrderRepository;
import com.saasplatform.common.enums.*;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.ForbiddenException;
import com.saasplatform.exception.ResourceNotFoundException;
import com.saasplatform.notification.entity.NotificationType;
import com.saasplatform.notification.service.NotificationService;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.plan.repository.PlanRepository;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.service.SubscriptionService;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrderService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final InvoiceService invoiceService;
    private final TaxCalculationService taxCalculationService;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public PaymentOrderDto createPaymentOrder(
            UUID userId,
            CreatePaymentOrderRequest request
    ) {
        if (request.getPlanCode() == PlanCode.FREE) {
            throw new BadRequestException("Free plan cannot be purchased via payment gateway");
        }

        Plan targetPlan = planRepository.findByCode(request.getPlanCode())
                .filter(Plan::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Target plan not found or inactive: " + request.getPlanCode()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Server-side authoritative calculation (Never trust client prices)
        BigDecimal basePrice = (request.getBillingInterval() == BillingInterval.YEARLY)
                ? targetPlan.getPriceYearly()
                : targetPlan.getPriceMonthly();

        TaxCalculationResult tax = taxCalculationService.calculateTax(basePrice);
        BigDecimal totalCharge = tax.getTotalAmount();

        PaymentGatewayService gatewayService = paymentGatewayFactory.getPaymentGatewayService();

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .user(user)
                .plan(targetPlan)
                .billingInterval(request.getBillingInterval())
                .amount(totalCharge)
                .currency(targetPlan.getCurrency() != null ? targetPlan.getCurrency() : "INR")
                .status(PaymentOrderStatus.CREATED)
                .gatewayProvider(gatewayService.getProvider())
                .gatewayOrderId("PENDING_" + UUID.randomUUID())
                .build();

        paymentOrder = paymentOrderRepository.save(paymentOrder);

        // Generate provider-specific gateway order
        String gatewayOrderId = gatewayService.createGatewayOrder(paymentOrder);
        paymentOrder.setGatewayOrderId(gatewayOrderId);
        paymentOrder = paymentOrderRepository.save(paymentOrder);

        auditLogService.logEvent(
                userId,
                user.getEmail(),
                AuditAction.PAYMENT_ORDER_CREATED,
                "PaymentOrder",
                paymentOrder.getId() != null ? paymentOrder.getId().toString() : "PENDING",
                "Created payment order for " + targetPlan.getName() + " (" + request.getBillingInterval() + ") amount=" + totalCharge,
                null
        );

        log.info("Created payment order id={} gatewayOrderId={} amount={} for userId={}",
                paymentOrder.getId(), gatewayOrderId, totalCharge, userId);

        return mapToDto(paymentOrder, gatewayService.getPublicKeyId());
    }

    @Transactional
    public PaymentVerificationResponse verifyPayment(
            UUID userId,
            VerifyPaymentRequest request
    ) {
        PaymentOrder paymentOrder = paymentOrderRepository.findByGatewayOrderId(request.getGatewayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found for gateway order: " + request.getGatewayOrderId()));

        if (!paymentOrder.getUser().getId().equals(userId)) {
            log.warn("Cross-user payment verification attempt: orderUserId={}, callerUserId={}",
                    paymentOrder.getUser().getId(), userId);
            throw new ForbiddenException("This payment order does not belong to your account");
        }

        if (paymentOrder.getStatus() == PaymentOrderStatus.PAID) {
            log.info("Payment order {} has already been settled", paymentOrder.getId());
            return PaymentVerificationResponse.builder()
                    .orderId(paymentOrder.getId())
                    .orderStatus(PaymentOrderStatus.PAID)
                    .planCode(paymentOrder.getPlan().getCode())
                    .planName(paymentOrder.getPlan().getName())
                    .subscriptionStatus(SubscriptionStatus.ACTIVE)
                    .totalAmount(paymentOrder.getAmount())
                    .message("Payment already verified and subscription is active")
                    .build();
        }

        if (paymentOrder.getStatus() == PaymentOrderStatus.FAILED || paymentOrder.getStatus() == PaymentOrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot verify a payment order that has been marked as " + paymentOrder.getStatus());
        }

        PaymentGatewayService gatewayService = paymentGatewayFactory.getServiceByProvider(paymentOrder.getGatewayProvider());
        boolean isValidSignature = gatewayService.verifyPaymentSignature(
                request.getGatewayOrderId(),
                request.getGatewayPaymentId(),
                request.getGatewaySignature()
        );

        if (!isValidSignature) {
            paymentOrder.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(paymentOrder);

            auditLogService.logEvent(
                    userId,
                    paymentOrder.getUser().getEmail(),
                    AuditAction.PAYMENT_FAILED,
                    "PaymentOrder",
                    paymentOrder.getId().toString(),
                    "Payment verification failed: invalid signature",
                    null
            );

            notificationService.createNotification(
                    paymentOrder.getUser(),
                    NotificationType.PAYMENT_FAILED,
                    "Payment Verification Failed",
                    "We could not verify your payment transaction. Please contact support if your account was debited.",
                    "{\"orderId\":\"" + paymentOrder.getId() + "\"}"
            );

            log.warn("Payment verification failed for orderId={}, invalid signature", paymentOrder.getId());
            throw new BadRequestException("Invalid payment signature received from gateway");
        }

        return settlePayment(paymentOrder, request.getGatewayPaymentId(), request.getGatewaySignature());
    }

    @Transactional
    public PaymentVerificationResponse settlePayment(
            PaymentOrder paymentOrder,
            String gatewayPaymentId,
            String gatewaySignature
    ) {
        if (paymentOrder.getStatus() == PaymentOrderStatus.PAID) {
            log.info("Payment order {} already settled, returning idempotent response", paymentOrder.getId());
            return PaymentVerificationResponse.builder()
                    .orderId(paymentOrder.getId())
                    .orderStatus(PaymentOrderStatus.PAID)
                    .planCode(paymentOrder.getPlan().getCode())
                    .planName(paymentOrder.getPlan().getName())
                    .subscriptionStatus(SubscriptionStatus.ACTIVE)
                    .totalAmount(paymentOrder.getAmount())
                    .message("Payment already settled")
                    .build();
        }

        // Settle Payment Order
        paymentOrder.setStatus(PaymentOrderStatus.PAID);
        paymentOrder.setGatewayPaymentId(gatewayPaymentId);
        paymentOrder.setGatewaySignature(gatewaySignature);
        paymentOrder.setPaidAt(LocalDateTime.now());
        paymentOrder = paymentOrderRepository.save(paymentOrder);

        // Upgrade active Subscription
        Subscription subscription = subscriptionService.upgradeSubscription(
                paymentOrder.getUser().getId(),
                paymentOrder.getPlan(),
                paymentOrder.getBillingInterval(),
                paymentOrder
        );

        // Generate immutable Invoice record
        Invoice invoice = invoiceService.generateInvoice(
                paymentOrder.getUser(),
                subscription,
                paymentOrder,
                paymentOrder.getPlan(),
                paymentOrder.getBillingInterval()
        );

        auditLogService.logEvent(
                paymentOrder.getUser().getId(),
                paymentOrder.getUser().getEmail(),
                AuditAction.PAYMENT_VERIFIED,
                "PaymentOrder",
                paymentOrder.getId().toString(),
                "Payment settled successfully: amount=" + paymentOrder.getAmount() + ", invoice=" + invoice.getInvoiceNumber(),
                null
        );

        notificationService.createNotification(
                paymentOrder.getUser(),
                NotificationType.PAYMENT_SUCCESS,
                "Payment Successful",
                String.format("Your payment of %s %s was successful. Tax invoice #%s has been generated.",
                        paymentOrder.getCurrency(), paymentOrder.getAmount(), invoice.getInvoiceNumber()),
                "{\"invoiceNumber\":\"" + invoice.getInvoiceNumber() + "\"}"
        );

        log.info("Successfully settled payment order={}, upgraded to plan={}, generated invoice={}",
                paymentOrder.getId(), paymentOrder.getPlan().getCode(), invoice.getInvoiceNumber());

        return PaymentVerificationResponse.builder()
                .orderId(paymentOrder.getId())
                .orderStatus(PaymentOrderStatus.PAID)
                .planCode(paymentOrder.getPlan().getCode())
                .planName(paymentOrder.getPlan().getName())
                .subscriptionStatus(subscription.getStatus())
                .invoiceNumber(invoice.getInvoiceNumber())
                .totalAmount(invoice.getTotalAmount())
                .message("Payment successful! Your subscription has been upgraded to " + paymentOrder.getPlan().getName())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PaymentOrderDto> getPaymentOrders(UUID userId) {
        PaymentGatewayService gatewayService = paymentGatewayFactory.getPaymentGatewayService();
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> mapToDto(order, gatewayService.getPublicKeyId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BillingConfigResponse getBillingConfig() {
        PaymentGatewayService gatewayService = paymentGatewayFactory.getPaymentGatewayService();
        return BillingConfigResponse.builder()
                .provider(gatewayService.getProvider())
                .keyId(gatewayService.getPublicKeyId())
                .currency("INR")
                .build();
    }

    private PaymentOrderDto mapToDto(PaymentOrder order, String keyId) {
        return PaymentOrderDto.builder()
                .id(order.getId())
                .planCode(order.getPlan().getCode())
                .planName(order.getPlan().getName())
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

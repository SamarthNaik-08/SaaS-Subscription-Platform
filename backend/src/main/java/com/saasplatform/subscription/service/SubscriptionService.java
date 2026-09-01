package com.saasplatform.subscription.service;

import com.saasplatform.audit.entity.AuditAction;
import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.common.enums.BillingInterval;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.common.enums.SubscriptionStatus;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.ResourceNotFoundException;
import com.saasplatform.notification.entity.NotificationType;
import com.saasplatform.notification.service.NotificationService;
import com.saasplatform.plan.dto.PlanDto;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.subscription.dto.SubscriptionDto;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.repository.SubscriptionRepository;
import com.saasplatform.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public SubscriptionDto getCurrentSubscriptionDto(UUID userId) {
        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for user"));
        return mapToSubscriptionDto(subscription);
    }

    @Transactional(readOnly = true)
    public Subscription getCurrentSubscription(UUID userId) {
        return subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for user"));
    }

    @Transactional
    public Subscription upgradeSubscription(
            UUID userId,
            Plan targetPlan,
            BillingInterval interval,
            PaymentOrder paymentOrder
    ) {
        log.info("Upgrading subscription for userId={} to plan={} interval={}",
                userId, targetPlan.getCode(), interval);

        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found for user: " + userId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodEnd = (interval == BillingInterval.YEARLY) ? now.plusYears(1) : now.plusMonths(1);

        subscription.setPlan(targetPlan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCancelledAt(null);
        subscription.setPaymentProvider(paymentOrder != null && paymentOrder.getGatewayProvider() != null
                ? paymentOrder.getGatewayProvider().name() : "GATEWAY");
        subscription.setExternalSubscriptionId(paymentOrder != null ? paymentOrder.getGatewayPaymentId() : null);

        subscription = subscriptionRepository.save(subscription);
        log.info("Successfully upgraded subscription id={} to {}", subscription.getId(), targetPlan.getCode());

        User user = subscription.getUser();
        auditLogService.logEvent(
                user.getId(),
                user.getEmail(),
                AuditAction.SUBSCRIPTION_UPGRADED,
                "Subscription",
                subscription.getId().toString(),
                "Upgraded to " + targetPlan.getName() + " (" + interval + ")",
                null
        );

        notificationService.createNotification(
                user,
                NotificationType.SUBSCRIPTION_UPGRADED,
                "Subscription Upgraded",
                String.format("Your subscription has been upgraded to %s (%s). Active until %s.", targetPlan.getName(), interval, periodEnd.toLocalDate()),
                "{\"plan\":\"" + targetPlan.getCode() + "\"}"
        );

        return subscription;
    }

    @Transactional
    public Subscription cancelSubscription(UUID userId) {
        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        if (subscription.getPlan().getCode() == PlanCode.FREE) {
            throw new BadRequestException("Free tier subscriptions cannot be cancelled");
        }

        if (subscription.isCancelAtPeriodEnd()) {
            throw new BadRequestException("Subscription is already scheduled for cancellation at the end of the billing period");
        }

        subscription.setCancelAtPeriodEnd(true);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription = subscriptionRepository.save(subscription);

        User user = subscription.getUser();
        auditLogService.logEvent(
                user.getId(),
                user.getEmail(),
                AuditAction.SUBSCRIPTION_CANCELLED,
                "Subscription",
                subscription.getId().toString(),
                "Subscription scheduled for cancellation at period end: " + subscription.getCurrentPeriodEnd(),
                null
        );

        notificationService.createNotification(
                user,
                NotificationType.SUBSCRIPTION_CANCELLED,
                "Subscription Cancellation Scheduled",
                String.format("Your subscription will end on %s. You can continue using your plan until then.", subscription.getCurrentPeriodEnd().toLocalDate()),
                "{}"
        );

        log.info("Scheduled subscription id={} cancellation at period end {}",
                subscription.getId(), subscription.getCurrentPeriodEnd());
        return subscription;
    }

    @Transactional
    public Subscription resumeSubscription(UUID userId) {
        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        if (!subscription.isCancelAtPeriodEnd()) {
            throw new BadRequestException("Subscription is not scheduled for cancellation");
        }

        if (subscription.getCurrentPeriodEnd().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("The billing period for this subscription has already elapsed. Please purchase a new subscription.");
        }

        subscription.setCancelAtPeriodEnd(false);
        subscription.setCancelledAt(null);
        subscription = subscriptionRepository.save(subscription);

        User user = subscription.getUser();
        auditLogService.logEvent(
                user.getId(),
                user.getEmail(),
                AuditAction.SUBSCRIPTION_RESUMED,
                "Subscription",
                subscription.getId().toString(),
                "Subscription resumed and set to auto-renew",
                null
        );

        notificationService.createNotification(
                user,
                NotificationType.SUBSCRIPTION_RESUMED,
                "Subscription Resumed",
                String.format("Your %s subscription is active and will renew on %s.", subscription.getPlan().getName(), subscription.getCurrentPeriodEnd().toLocalDate()),
                "{}"
        );

        log.info("Resumed active subscription id={}", subscription.getId());
        return subscription;
    }

    public SubscriptionDto mapToSubscriptionDto(Subscription subscription) {
        return SubscriptionDto.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .plan(PlanDto.fromEntity(subscription.getPlan()))
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .cancelAtPeriodEnd(subscription.isCancelAtPeriodEnd())
                .cancelledAt(subscription.getCancelledAt())
                .paymentProvider(subscription.getPaymentProvider())
                .externalSubscriptionId(subscription.getExternalSubscriptionId())
                .build();
    }
}

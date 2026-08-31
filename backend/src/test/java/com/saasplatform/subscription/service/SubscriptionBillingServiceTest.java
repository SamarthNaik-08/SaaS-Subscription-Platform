package com.saasplatform.subscription.service;

import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.common.enums.BillingInterval;
import com.saasplatform.common.enums.PaymentGatewayProvider;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.common.enums.SubscriptionStatus;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.notification.service.NotificationService;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.repository.SubscriptionRepository;
import com.saasplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionBillingServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UUID userId;
    private User user;
    private Plan freePlan;
    private Plan proPlan;
    private Subscription activeSubscription;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder().id(userId).email("user@saas.com").firstName("Alice").lastName("User").build();

        freePlan = Plan.builder().id(UUID.randomUUID()).code(PlanCode.FREE).name("Free Plan").priceMonthly(BigDecimal.ZERO).priceYearly(BigDecimal.ZERO).monthlyAiLimit(50).storageLimitMb(100L).build();
        proPlan = Plan.builder().id(UUID.randomUUID()).code(PlanCode.PRO).name("Pro Plan").priceMonthly(new BigDecimal("499.00")).priceYearly(new BigDecimal("4990.00")).monthlyAiLimit(1000).storageLimitMb(5120L).build();

        activeSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now().minusDays(5))
                .currentPeriodEnd(LocalDateTime.now().plusDays(25))
                .build();
    }

    @Test
    void shouldUpgradeSubscriptionFromFreeToPro() {
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .gatewayProvider(PaymentGatewayProvider.SANDBOX)
                .gatewayPaymentId("pay_12345")
                .build();

        when(subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Subscription upgraded = subscriptionService.upgradeSubscription(userId, proPlan, BillingInterval.MONTHLY, paymentOrder);

        assertNotNull(upgraded);
        assertEquals(PlanCode.PRO, upgraded.getPlan().getCode());
        assertEquals("SANDBOX", upgraded.getPaymentProvider());
        assertEquals("pay_12345", upgraded.getExternalSubscriptionId());
        assertFalse(upgraded.isCancelAtPeriodEnd());
    }

    @Test
    void shouldScheduleSubscriptionCancellationAtPeriodEnd() {
        activeSubscription.setPlan(proPlan);
        when(subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Subscription cancelled = subscriptionService.cancelSubscription(userId);

        assertTrue(cancelled.isCancelAtPeriodEnd());
        assertNotNull(cancelled.getCancelledAt());
    }

    @Test
    void shouldRejectCancellingFreeSubscription() {
        activeSubscription.setPlan(freePlan);
        when(subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(activeSubscription));

        assertThrows(BadRequestException.class, () ->
                subscriptionService.cancelSubscription(userId));
    }

    @Test
    void shouldResumeCancelledSubscription() {
        activeSubscription.setPlan(proPlan);
        activeSubscription.setCancelAtPeriodEnd(true);
        activeSubscription.setCancelledAt(LocalDateTime.now().minusDays(1));
        activeSubscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(10));

        when(subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Subscription resumed = subscriptionService.resumeSubscription(userId);

        assertFalse(resumed.isCancelAtPeriodEnd());
        assertNull(resumed.getCancelledAt());
    }
}

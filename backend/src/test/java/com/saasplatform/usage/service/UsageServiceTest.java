package com.saasplatform.usage.service;

import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.common.enums.SubscriptionStatus;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.QuotaExceededException;
import com.saasplatform.notification.service.NotificationService;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.repository.SubscriptionRepository;
import com.saasplatform.usage.dto.CurrentUsageResponse;
import com.saasplatform.usage.dto.MetricUsageDto;
import com.saasplatform.usage.entity.UsageRecord;
import com.saasplatform.usage.repository.UsageRecordRepository;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
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
class UsageServiceTest {

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UsageService usageService;

    private UUID userId;
    private User user;
    private Plan freePlan;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("test@saas.com").firstName("Alice").lastName("User").build();

        freePlan = Plan.builder()
                .id(UUID.randomUUID())
                .code(PlanCode.FREE)
                .name("Free Plan")
                .priceMonthly(BigDecimal.ZERO)
                .monthlyAiLimit(50)
                .storageLimitMb(100L)
                .build();

        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now().minusDays(5))
                .currentPeriodEnd(LocalDateTime.now().plusDays(25))
                .build();
    }

    @Test
    void shouldRecordUsageWithinQuota() {
        when(subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findByIdForUpdate(subscription.getId())).thenReturn(Optional.of(subscription));
        when(usageRecordRepository.sumQuantityByUserAndMetricAndPeriod(eq(userId), eq(UsageMetric.AI_REQUEST), any(), any()))
                .thenReturn(10L);

        MetricUsageDto result = usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 5L, "Test inference");

        assertNotNull(result);
        assertEquals(15L, result.getUsed());
        assertEquals(50L, result.getLimit());
        assertEquals(35L, result.getRemaining());
        assertEquals(30.0, result.getPercentage());
        verify(usageRecordRepository).save(any(UsageRecord.class));
    }

    @Test
    void shouldRejectZeroOrNegativeQuantity() {
        assertThrows(BadRequestException.class, () ->
                usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 0L, "Zero usage"));

        assertThrows(BadRequestException.class, () ->
                usageService.recordUsage(userId, UsageMetric.AI_REQUEST, -5L, "Negative usage"));
    }

    @Test
    void shouldRejectExcessiveBatchQuantity() {
        assertThrows(BadRequestException.class, () ->
                usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 2_000_000L, "Excessive batch"));
    }

    @Test
    void shouldRejectNullMetric() {
        assertThrows(BadRequestException.class, () ->
                usageService.recordUsage(userId, null, 1L, "Missing metric"));
    }

    @Test
    void shouldAllowUsageUpToExactLimitAndRejectNext() {
        when(subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findByIdForUpdate(subscription.getId())).thenReturn(Optional.of(subscription));
        when(usageRecordRepository.sumQuantityByUserAndMetricAndPeriod(eq(userId), eq(UsageMetric.AI_REQUEST), any(), any()))
                .thenReturn(48L);

        // Exactly hits 50/50
        MetricUsageDto exactLimitResult = usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 2L, "Hits exact limit");
        assertEquals(50L, exactLimitResult.getUsed());
        assertEquals(0L, exactLimitResult.getRemaining());
        assertEquals(100.0, exactLimitResult.getPercentage());

        // Next request of 1 when current is 50 throws QuotaExceededException (429)
        when(usageRecordRepository.sumQuantityByUserAndMetricAndPeriod(eq(userId), eq(UsageMetric.AI_REQUEST), any(), any()))
                .thenReturn(50L);

        assertThrows(QuotaExceededException.class, () ->
                usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 1L, "Exceeds limit"));
    }

    @Test
    void shouldCalculateCurrentUsagePercentagesCorrectly() {
        when(subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(subscription));
        when(usageRecordRepository.sumQuantityByUserAndMetricAndPeriod(eq(userId), eq(UsageMetric.AI_REQUEST), any(), any()))
                .thenReturn(25L);
        when(usageRecordRepository.sumQuantityByUserAndMetricAndPeriod(eq(userId), eq(UsageMetric.STORAGE), any(), any()))
                .thenReturn(52428800L); // 50 MB in bytes (50/100 MB = 50%)

        CurrentUsageResponse response = usageService.getCurrentUsage(userId);

        assertNotNull(response);
        assertNotNull(response.getMetrics());

        MetricUsageDto aiDto = response.getMetrics().get("AI_REQUEST");
        assertEquals(25L, aiDto.getUsed());
        assertEquals(50L, aiDto.getLimit());
        assertEquals(25L, aiDto.getRemaining());
        assertEquals(50.0, aiDto.getPercentage());

        MetricUsageDto storageDto = response.getMetrics().get("STORAGE");
        assertEquals(52428800L, storageDto.getUsed());
        assertEquals(100L * 1024 * 1024, storageDto.getLimit());
        assertEquals(50.0, storageDto.getPercentage());
    }
}

package com.saasplatform.usage.service;

import com.saasplatform.audit.entity.AuditAction;
import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.QuotaExceededException;
import com.saasplatform.exception.ResourceNotFoundException;
import com.saasplatform.notification.service.NotificationService;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.repository.SubscriptionRepository;
import com.saasplatform.usage.dto.*;
import com.saasplatform.usage.entity.UsageRecord;
import com.saasplatform.usage.repository.UsageRecordRepository;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageService {

    private final UsageRecordRepository usageRecordRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public MetricUsageDto consume(UUID userId, UsageMetric metric, long quantity, String metadata) {
        return recordUsage(userId, metric, quantity, metadata);
    }

    @Transactional
    public MetricUsageDto recordUsage(UUID userId, UsageMetric metric, long quantity, String metadata) {
        if (metric == null) {
            throw new BadRequestException("Usage metric is required");
        }
        if (quantity <= 0) {
            throw new BadRequestException("Usage quantity must be strictly positive");
        }
        if (quantity > 1_000_000L) {
            throw new BadRequestException("Usage quantity exceeds maximum permitted batch limit of 1,000,000");
        }

        log.info("Recording usage: userId={}, metric={}, quantity={}", userId, metric, quantity);

        Subscription activeSub = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for user: " + userId));

        // Atomic lock on the subscription row to serialize concurrent quota checks
        Subscription lockedSub = subscriptionRepository.findByIdForUpdate(activeSub.getId())
                .orElse(activeSub);

        Plan plan = lockedSub.getPlan();
        LocalDateTime periodStart = lockedSub.getCurrentPeriodStart();
        LocalDateTime periodEnd = lockedSub.getCurrentPeriodEnd();

        long currentUsed = usageRecordRepository.sumQuantityByUserAndMetricAndPeriod(
                userId,
                metric,
                periodStart,
                periodEnd
        );

        long limit = getMetricLimit(plan, metric);

        if (currentUsed + quantity > limit) {
            log.warn("Atomic quota check failed for user {}: used={}, requested={}, limit={}",
                    userId, currentUsed, quantity, limit);

            User user = lockedSub.getUser();
            auditLogService.logEvent(
                    userId,
                    user != null ? user.getEmail() : null,
                    AuditAction.QUOTA_EXCEEDED,
                    "UsageRecord",
                    null,
                    String.format("Quota limit exceeded for %s: %d/%d used, requested %d", metric.name(), currentUsed, limit, quantity),
                    null
            );

            if (user != null) {
                notificationService.checkAndSendQuotaThresholdAlert(user, currentUsed + quantity, limit, periodStart);
            }

            throw new QuotaExceededException(
                    String.format("Quota limit exceeded for %s (%d/%d used). Upgrade your plan to increase limits.",
                            metric.name(), currentUsed, limit)
            );
        }

        User user = lockedSub.getUser();
        if (user == null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        }

        UsageRecord record = UsageRecord.builder()
                .user(user)
                .metric(metric)
                .quantity(quantity)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .metadata(metadata)
                .build();

        usageRecordRepository.save(record);

        long newTotal = currentUsed + quantity;
        long remaining = Math.max(0, limit - newTotal);
        double percentage = limit > 0 ? (newTotal * 100.0) / limit : 0.0;

        // Check threshold notifications (75%, 90%, 100%) idempotently
        notificationService.checkAndSendQuotaThresholdAlert(user, newTotal, limit, periodStart);

        return MetricUsageDto.builder()
                .used(newTotal)
                .limit(limit)
                .remaining(remaining)
                .percentage(Math.round(percentage * 100.0) / 100.0)
                .build();
    }

    @Transactional(readOnly = true)
    public CurrentUsageResponse getCurrentUsage(UUID userId) {
        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for user"));

        Plan plan = subscription.getPlan();
        LocalDateTime periodStart = subscription.getCurrentPeriodStart();
        LocalDateTime periodEnd = subscription.getCurrentPeriodEnd();

        Map<String, MetricUsageDto> metrics = new HashMap<>();

        // 1. AI_REQUEST
        long aiUsed = usageRecordRepository.sumQuantityByUserAndMetricAndPeriod(
                userId,
                UsageMetric.AI_REQUEST,
                periodStart,
                periodEnd
        );
        long aiLimit = plan.getMonthlyAiLimit();
        long aiRemaining = Math.max(0, aiLimit - aiUsed);
        double aiPercentage = aiLimit > 0 ? (aiUsed * 100.0) / aiLimit : 0.0;

        metrics.put(UsageMetric.AI_REQUEST.name(), MetricUsageDto.builder()
                .used(aiUsed)
                .limit(aiLimit)
                .remaining(aiRemaining)
                .percentage(Math.round(aiPercentage * 100.0) / 100.0)
                .build());

        // 2. STORAGE (in Bytes)
        long storageUsedBytes = usageRecordRepository.sumQuantityByUserAndMetricAndPeriod(
                userId,
                UsageMetric.STORAGE,
                periodStart,
                periodEnd
        );
        long storageLimitBytes = plan.getStorageLimitMb() * 1024L * 1024L;
        long storageRemainingBytes = Math.max(0, storageLimitBytes - storageUsedBytes);
        double storagePercentage = storageLimitBytes > 0 ? (storageUsedBytes * 100.0) / storageLimitBytes : 0.0;

        metrics.put(UsageMetric.STORAGE.name(), MetricUsageDto.builder()
                .used(storageUsedBytes)
                .limit(storageLimitBytes)
                .remaining(storageRemainingBytes)
                .percentage(Math.round(storagePercentage * 100.0) / 100.0)
                .build());

        return CurrentUsageResponse.builder()
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .metrics(metrics)
                .build();
    }

    @Transactional(readOnly = true)
    public UsageHistoryResponse getUsageHistory(UUID userId, UsageMetric metric) {
        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found"));

        LocalDateTime periodStart = subscription.getCurrentPeriodStart();
        LocalDateTime periodEnd = subscription.getCurrentPeriodEnd();

        List<UsageRecord> records = (metric != null)
                ? usageRecordRepository.findTop50ByUserIdAndMetricOrderByCreatedAtDesc(userId, metric)
                : usageRecordRepository.findByUserIdAndPeriod(userId, periodStart, periodEnd);

        long totalUsed = records.stream().mapToLong(UsageRecord::getQuantity).sum();

        List<UsageRecordDto> recordDtos = records.stream()
                .map(r -> UsageRecordDto.builder()
                        .id(r.getId())
                        .metric(r.getMetric())
                        .quantity(r.getQuantity())
                        .createdAt(r.getCreatedAt())
                        .metadata(r.getMetadata())
                        .userName(r.getUser() != null ? r.getUser().getFirstName() + " " + r.getUser().getLastName() : "User")
                        .userEmail(r.getUser() != null ? r.getUser().getEmail() : null)
                        .build())
                .collect(Collectors.toList());

        return UsageHistoryResponse.builder()
                .metric(metric)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .totalUsed(totalUsed)
                .records(recordDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public UsageSummaryResponse getUsageSummary(UUID userId) {
        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found"));

        CurrentUsageResponse currentUsage = getCurrentUsage(userId);

        return UsageSummaryResponse.builder()
                .userEmail(subscription.getUser().getEmail())
                .planName(subscription.getPlan().getName())
                .planCode(subscription.getPlan().getCode().name())
                .subscriptionStatus(subscription.getStatus())
                .periodStart(currentUsage.getPeriodStart())
                .periodEnd(currentUsage.getPeriodEnd())
                .metrics(currentUsage.getMetrics())
                .build();
    }

    private long getMetricLimit(Plan plan, UsageMetric metric) {
        return switch (metric) {
            case AI_REQUEST -> plan.getMonthlyAiLimit();
            case STORAGE -> plan.getStorageLimitMb() * 1024L * 1024L;
        };
    }
}

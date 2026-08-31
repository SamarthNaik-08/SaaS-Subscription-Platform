package com.saasplatform.usage.service;

import com.saasplatform.common.enums.GlobalRole;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.common.enums.SubscriptionStatus;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.exception.QuotaExceededException;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.plan.repository.PlanRepository;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.repository.SubscriptionRepository;
import com.saasplatform.usage.repository.UsageRecordRepository;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UsageConcurrencyIntegrationTest {

    @Autowired
    private UsageService usageService;

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Subscription subscription;
    private Plan plan;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                User.builder()
                        .email("concurrency-" + UUID.randomUUID() + "@test.com")
                        .passwordHash("hashed")
                        .firstName("Test")
                        .lastName("User")
                        .globalRole(GlobalRole.USER)
                        .build()
        );

        plan = planRepository.findByCode(PlanCode.FREE).orElseGet(() ->
                planRepository.save(
                        Plan.builder()
                                .code(PlanCode.FREE)
                                .name("Free Plan")
                                .priceMonthly(BigDecimal.ZERO)
                                .priceYearly(BigDecimal.ZERO)
                                .monthlyAiLimit(50)
                                .storageLimitMb(100L)
                                .build()
                )
        );

        subscription = subscriptionRepository.save(
                Subscription.builder()
                        .user(user)
                        .plan(plan)
                        .status(SubscriptionStatus.ACTIVE)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .currentPeriodStart(LocalDateTime.now().minusDays(1))
                        .currentPeriodEnd(LocalDateTime.now().plusDays(29))
                        .build()
        );
    }

    @Autowired
    private com.saasplatform.notification.repository.NotificationRepository notificationRepository;

    @Autowired
    private com.saasplatform.audit.repository.AuditLogRepository auditLogRepository;

    @AfterEach
    void tearDown() {
        usageRecordRepository.deleteAll();
        subscriptionRepository.deleteAll();
        notificationRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testConcurrentQuotaExhaustionRaceCondition() throws InterruptedException {
        // Pre-record 48 usage units against limit of 50
        usageService.recordUsage(user.getId(), UsageMetric.AI_REQUEST, 48L, "Initial 48 consumption");

        // Two concurrent requests each attempting to consume 2 (48 + 2 = 50 succeeds; 50 + 2 = 52 fails)
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger quotaExceededCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    usageService.recordUsage(user.getId(), UsageMetric.AI_REQUEST, 2L, "Concurrent increment of 2");
                    successCount.incrementAndGet();
                } catch (QuotaExceededException e) {
                    quotaExceededCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start both threads simultaneously
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly one should succeed (reaching 50) and one should receive QuotaExceededException (429)
        assertEquals(0, errors.size(), "Unexpected errors: " + errors);
        assertEquals(1, successCount.get(), "Exactly 1 concurrent request must succeed to reach 50");
        assertEquals(1, quotaExceededCount.get(), "Exactly 1 concurrent request must fail with QuotaExceededException");

        // Total recorded usage in database must be exactly 50 (48 + 2)
        long finalUsage = usageRecordRepository.sumQuantityByUserAndMetricAndPeriod(
                user.getId(),
                UsageMetric.AI_REQUEST,
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd()
        );
        assertEquals(50L, finalUsage);

        // Any further request of 1 must immediately fail with QuotaExceededException
        assertThrows(QuotaExceededException.class, () ->
                usageService.recordUsage(user.getId(), UsageMetric.AI_REQUEST, 1L, "Subsequent request")
        );
    }

    @Test
    void testTenConcurrentThreadsCompetingForLimitedQuota() throws InterruptedException {
        // Limit is 50. Total remaining is 20 (we start with 30).
        usageService.recordUsage(user.getId(), UsageMetric.AI_REQUEST, 30L, "Initial 30 units");

        // 10 concurrent threads each attempting to consume 10 units (need 100 units, only 20 available)
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger quotaExceededCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    usageService.recordUsage(user.getId(), UsageMetric.AI_REQUEST, 10L, "Batch 10");
                    successCount.incrementAndGet();
                } catch (QuotaExceededException e) {
                    quotaExceededCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly 2 requests of 10 should succeed (reaching 50), and 8 must fail with QuotaExceededException (429)
        assertEquals(2, successCount.get(), "Exactly 2 requests of 10 should succeed to reach 50");
        assertEquals(8, quotaExceededCount.get(), "Exactly 8 requests should fail with 429");

        long totalConsumed = usageRecordRepository.sumQuantityByUserAndMetricAndPeriod(
                user.getId(),
                UsageMetric.AI_REQUEST,
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd()
        );
        assertEquals(50L, totalConsumed, "Final consumed usage must be exactly 50");
    }
}

package com.saasplatform.admin.service;

import com.saasplatform.admin.dto.AdminAnalyticsDto;
import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.billing.repository.PaymentOrderRepository;
import com.saasplatform.common.enums.PaymentOrderStatus;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.common.enums.SubscriptionStatus;
import com.saasplatform.common.enums.UserStatus;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.repository.SubscriptionRepository;
import com.saasplatform.usage.entity.UsageRecord;
import com.saasplatform.usage.repository.UsageRecordRepository;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final UsageRecordRepository usageRecordRepository;

    @Transactional(readOnly = true)
    public AdminAnalyticsDto calculateAnalytics() {
        log.info("Calculating authoritative SaaS analytics from PostgreSQL records");

        List<User> allUsers = userRepository.findAll();
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();

        List<Subscription> allSubscriptions = subscriptionRepository.findAll();
        long totalSubscriptions = allSubscriptions.size();

        long freeUsers = allSubscriptions.stream()
                .filter(s -> s.getPlan().getCode() == PlanCode.FREE && s.getStatus() == SubscriptionStatus.ACTIVE)
                .count();

        long proUsers = allSubscriptions.stream()
                .filter(s -> s.getPlan().getCode() == PlanCode.PRO && s.getStatus() == SubscriptionStatus.ACTIVE)
                .count();

        long businessUsers = allSubscriptions.stream()
                .filter(s -> s.getPlan().getCode() == PlanCode.BUSINESS && s.getStatus() == SubscriptionStatus.ACTIVE)
                .count();

        long activePaidSubscribers = proUsers + businessUsers;

        long cancelledSubscriptions = allSubscriptions.stream()
                .filter(Subscription::isCancelAtPeriodEnd)
                .count();

        // Calculate Authoritative MRR
        BigDecimal mrr = BigDecimal.ZERO;
        for (Subscription sub : allSubscriptions) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE && sub.getPlan().getCode() != PlanCode.FREE) {
                // If yearly period (> 40 days between start and end), divide yearly price by 12
                long days = java.time.Duration.between(sub.getCurrentPeriodStart(), sub.getCurrentPeriodEnd()).toDays();
                if (days > 40) {
                    BigDecimal monthlyShare = sub.getPlan().getPriceYearly().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                    mrr = mrr.add(monthlyShare);
                } else {
                    mrr = mrr.add(sub.getPlan().getPriceMonthly());
                }
            }
        }

        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP);

        // Calculate Total Revenue from settled Payment Orders
        List<PaymentOrder> allOrders = paymentOrderRepository.findAll();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long paymentSuccessCount = 0;
        long paymentFailureCount = 0;
        Map<String, BigDecimal> revenueByPlan = new HashMap<>();

        for (PaymentOrder order : allOrders) {
            if (order.getStatus() == PaymentOrderStatus.PAID) {
                totalRevenue = totalRevenue.add(order.getAmount());
                paymentSuccessCount++;

                String planKey = order.getPlan() != null ? order.getPlan().getCode().name() : "OTHER";
                revenueByPlan.put(planKey, revenueByPlan.getOrDefault(planKey, BigDecimal.ZERO).add(order.getAmount()));
            } else if (order.getStatus() == PaymentOrderStatus.FAILED) {
                paymentFailureCount++;
            }
        }

        long totalSettledOrFailed = paymentSuccessCount + paymentFailureCount;
        double paymentSuccessRate = totalSettledOrFailed > 0
                ? Math.round(((double) paymentSuccessCount / totalSettledOrFailed) * 10000.0) / 100.0
                : 100.0;

        double conversionRate = totalUsers > 0
                ? Math.round(((double) activePaidSubscribers / totalUsers) * 10000.0) / 100.0
                : 0.0;

        double churnRate = totalSubscriptions > 0
                ? Math.round(((double) cancelledSubscriptions / totalSubscriptions) * 10000.0) / 100.0
                : 0.0;

        BigDecimal arppu = activePaidSubscribers > 0
                ? mrr.divide(BigDecimal.valueOf(activePaidSubscribers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Total AI Usage
        List<UsageRecord> usageRecords = usageRecordRepository.findAll();
        long totalAiUsage = usageRecords.stream().mapToLong(UsageRecord::getQuantity).sum();

        Map<String, Long> usageByPlan = new HashMap<>();
        usageByPlan.put("FREE", freeUsers);
        usageByPlan.put("PRO", proUsers);
        usageByPlan.put("BUSINESS", businessUsers);

        return AdminAnalyticsDto.builder()
                .mrr(mrr.setScale(2, RoundingMode.HALF_UP))
                .arr(arr)
                .totalRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP))
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .activePaidSubscribers(activePaidSubscribers)
                .freeUsers(freeUsers)
                .proUsers(proUsers)
                .businessUsers(businessUsers)
                .conversionRate(conversionRate)
                .churnRate(churnRate)
                .arppu(arppu)
                .totalAiUsage(totalAiUsage)
                .paymentSuccessCount(paymentSuccessCount)
                .paymentFailureCount(paymentFailureCount)
                .paymentSuccessRate(paymentSuccessRate)
                .revenueByPlan(revenueByPlan)
                .usageByPlan(usageByPlan)
                .build();
    }
}

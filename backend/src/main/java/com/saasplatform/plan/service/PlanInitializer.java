package com.saasplatform.plan.service;

import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanInitializer implements CommandLineRunner {

    private final PlanRepository planRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking and seeding default subscription plans...");

        seedPlanIfAbsent(
                PlanCode.FREE,
                "Free Plan",
                "Essential starter plan for individuals to explore the platform",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "INR",
                50,
                100L
        );

        seedPlanIfAbsent(
                PlanCode.PRO,
                "Pro Plan",
                "Professional tier for power creators and developers",
                new BigDecimal("499.00"),
                new BigDecimal("4990.00"),
                "INR",
                1000,
                5120L // 5 GB
        );

        seedPlanIfAbsent(
                PlanCode.BUSINESS,
                "Business Plan",
                "Enterprise tier with highest throughput and dedicated quotas",
                new BigDecimal("1499.00"),
                new BigDecimal("14990.00"),
                "INR",
                5000,
                51200L // 50 GB
        );

        log.info("Plan seeding completed. Total active plans: {}", planRepository.count());
    }

    private void seedPlanIfAbsent(
            PlanCode code,
            String name,
            String description,
            BigDecimal priceMonthly,
            BigDecimal priceYearly,
            String currency,
            Integer monthlyAiLimit,
            Long storageLimitMb
    ) {
        if (!planRepository.existsByCode(code)) {
            Plan plan = Plan.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .priceMonthly(priceMonthly)
                    .priceYearly(priceYearly)
                    .currency(currency)
                    .monthlyAiLimit(monthlyAiLimit)
                    .storageLimitMb(storageLimitMb)
                    .isActive(true)
                    .build();
            planRepository.save(plan);
            log.info("Seeded plan: {}", code);
        }
    }
}

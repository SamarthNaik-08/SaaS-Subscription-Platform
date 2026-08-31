package com.saasplatform.user.controller;

import com.saasplatform.billing.entity.Invoice;
import com.saasplatform.billing.repository.InvoiceRepository;
import com.saasplatform.common.enums.GlobalRole;
import com.saasplatform.common.enums.InvoiceStatus;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.common.enums.SubscriptionStatus;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.plan.repository.PlanRepository;
import com.saasplatform.security.JwtService;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.repository.SubscriptionRepository;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserIsolationAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JwtService jwtService;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private Invoice invoiceB;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder()
                .email("usera-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .firstName("User")
                .lastName("A")
                .globalRole(GlobalRole.USER)
                .build());

        userB = userRepository.save(User.builder()
                .email("userb-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .firstName("User")
                .lastName("B")
                .globalRole(GlobalRole.USER)
                .build());

        tokenA = jwtService.generateAccessToken(userA.getId(), userA.getEmail(), "USER");
        tokenB = jwtService.generateAccessToken(userB.getId(), userB.getEmail(), "USER");

        Plan plan = planRepository.findByCode(PlanCode.FREE).orElseGet(() ->
                planRepository.save(Plan.builder()
                        .code(PlanCode.FREE)
                        .name("Free Plan")
                        .priceMonthly(BigDecimal.ZERO)
                        .priceYearly(BigDecimal.ZERO)
                        .monthlyAiLimit(50)
                        .storageLimitMb(100L)
                        .build())
        );

        Subscription subB = subscriptionRepository.save(Subscription.builder()
                .user(userB)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(10))
                .currentPeriodStart(LocalDateTime.now().minusDays(10))
                .currentPeriodEnd(LocalDateTime.now().plusDays(20))
                .build());

        invoiceB = invoiceRepository.save(Invoice.builder()
                .invoiceNumber("INV-B-" + UUID.randomUUID())
                .user(userB)
                .subscription(subB)
                .subtotal(new BigDecimal("499.00"))
                .taxAmount(new BigDecimal("89.82"))
                .totalAmount(new BigDecimal("588.82"))
                .currency("INR")
                .status(InvoiceStatus.PAID)
                .billingPeriodStart(LocalDateTime.now().minusDays(10))
                .billingPeriodEnd(LocalDateTime.now().plusDays(20))
                .build());
    }

    @Test
    void userACannotAccessUserBInvoice() throws Exception {
        // User A tries to access User B's invoice ID -> Should return 404 (ResourceNotFoundException)
        mockMvc.perform(get("/api/v1/billing/invoices/" + invoiceB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        // User B can access their own invoice -> Should return 200 OK
        mockMvc.perform(get("/api/v1/billing/invoices/" + invoiceB.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
    }
}

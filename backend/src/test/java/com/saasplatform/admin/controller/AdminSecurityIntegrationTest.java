package com.saasplatform.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.admin.dto.UpdatePlanRequest;
import com.saasplatform.common.enums.GlobalRole;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.plan.repository.PlanRepository;
import com.saasplatform.security.JwtService;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User normalUser;
    private User adminUser;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        normalUser = userRepository.save(User.builder()
                .email("user-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .firstName("Normal")
                .lastName("User")
                .globalRole(GlobalRole.USER)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .firstName("Platform")
                .lastName("Admin")
                .globalRole(GlobalRole.ADMIN)
                .build());

        userToken = jwtService.generateAccessToken(normalUser.getId(), normalUser.getEmail(), "USER");
        adminToken = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");
    }

    @Test
    void shouldReturn401WhenUnauthenticatedAccessToAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenNormalUserAccessesAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/analytics")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminAccessToDashboardAndAnalytics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").isNumber());

        mockMvc.perform(get("/api/v1/admin/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mrr").exists())
                .andExpect(jsonPath("$.data.arr").exists());
    }

    @Test
    void shouldAllowAdminToUpdatePlan() throws Exception {
        Plan plan = planRepository.findByCode(PlanCode.PRO).orElseGet(() ->
                planRepository.save(Plan.builder()
                        .code(PlanCode.PRO)
                        .name("Pro Plan")
                        .priceMonthly(new BigDecimal("499.00"))
                        .priceYearly(new BigDecimal("4990.00"))
                        .monthlyAiLimit(1000)
                        .storageLimitMb(5120L)
                        .isActive(true)
                        .build())
        );

        UpdatePlanRequest updateRequest = UpdatePlanRequest.builder()
                .name("Pro Plus Plan")
                .description("Updated pro plan")
                .priceMonthly(new BigDecimal("599.00"))
                .priceYearly(new BigDecimal("5990.00"))
                .monthlyAiLimit(2000)
                .storageLimitMb(10240L)
                .isActive(true)
                .build();

        mockMvc.perform(put("/api/v1/admin/plans/" + plan.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Pro Plus Plan"))
                .andExpect(jsonPath("$.data.monthlyAiLimit").value(2000));
    }
}

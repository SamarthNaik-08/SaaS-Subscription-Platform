package com.saasplatform.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.ai.research.dto.DeepResearchRequest;
import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.auth.service.AuthService;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.usage.service.UsageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiDeepResearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsageService usageService;

    @Test
    void shouldRejectUnauthenticatedResearchWith401() throws Exception {
        DeepResearchRequest req = DeepResearchRequest.builder()
                .topic("Autonomous AI agents in healthcare")
                .depth(1)
                .maxQueries(3)
                .build();

        mockMvc.perform(post("/api/v1/ai/research")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectBlankTopicWith400() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Research")
                .lastName("Tester")
                .email("blank-research-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        DeepResearchRequest req = DeepResearchRequest.builder()
                .topic("   ")
                .build();

        mockMvc.perform(post("/api/v1/ai/research")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectInvalidMaxQueriesWith400() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Queries")
                .lastName("Tester")
                .email("queries-bound-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        DeepResearchRequest req = DeepResearchRequest.builder()
                .topic("Scalable vector search")
                .maxQueries(25) // Max allowed is 8
                .build();

        mockMvc.perform(post("/api/v1/ai/research")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldPerformAuthenticatedDeepResearchAndReturnStructuredReport() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Principal")
                .lastName("Researcher")
                .email("deep-researcher-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        DeepResearchRequest req = DeepResearchRequest.builder()
                .topic("Next-generation transformer attention optimizations")
                .depth(2)
                .maxQueries(4)
                .model("gemini-2.0-flash")
                .build();

        mockMvc.perform(post("/api/v1/ai/research")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.topic").isNotEmpty())
                .andExpect(jsonPath("$.data.executiveSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.detailedAnalysis").isNotEmpty())
                .andExpect(jsonPath("$.data.sources").isArray())
                .andExpect(jsonPath("$.data.citations").isArray())
                .andExpect(jsonPath("$.data.totalQueriesExecuted").isNumber())
                .andExpect(jsonPath("$.data.quotaUsage.used").value(1))
                .andExpect(jsonPath("$.data.quotaUsage.remaining").value(49));
    }

    @Test
    void shouldRejectResearchWhenQuotaExhaustedWith429() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Exhausted")
                .lastName("Researcher")
                .email("exhausted-researcher-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();
        // Exhaust 50 free credits
        usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 50L, "Exhaust free quota");

        DeepResearchRequest req = DeepResearchRequest.builder()
                .topic("Deep research topic when out of quota")
                .build();

        mockMvc.perform(post("/api/v1/ai/research")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("QUOTA_EXCEEDED"));
    }
}

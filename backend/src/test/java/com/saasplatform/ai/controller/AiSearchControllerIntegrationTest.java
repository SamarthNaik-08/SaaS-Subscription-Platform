package com.saasplatform.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.ai.search.dto.AiSearchGenerateRequest;
import com.saasplatform.ai.search.dto.WebSearchRequest;
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
class AiSearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsageService usageService;

    @Test
    void shouldRejectUnauthenticatedSearchWith401() throws Exception {
        WebSearchRequest req = WebSearchRequest.builder()
                .query("Latest technology trends")
                .maxResults(5)
                .build();

        mockMvc.perform(post("/api/v1/ai/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectBlankQueryWith400() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Search")
                .lastName("User")
                .email("blank-search-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        WebSearchRequest req = WebSearchRequest.builder()
                .query("   ")
                .build();

        mockMvc.perform(post("/api/v1/ai/search")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectMaxResultsOutOfBoundsWith400() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Bound")
                .lastName("Tester")
                .email("bound-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        WebSearchRequest req = WebSearchRequest.builder()
                .query("Valid query")
                .maxResults(50) // Max allowed is 10
                .build();

        mockMvc.perform(post("/api/v1/ai/search")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldPerformAuthenticatedSearch() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Live")
                .lastName("Searcher")
                .email("live-search-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        WebSearchRequest req = WebSearchRequest.builder()
                .query("Artificial intelligence breakthrough")
                .maxResults(3)
                .build();

        mockMvc.perform(post("/api/v1/ai/search")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sources").isArray())
                .andExpect(jsonPath("$.data.sources.length()").value(3))
                .andExpect(jsonPath("$.data.sources[0].id").value("S1"))
                .andExpect(jsonPath("$.data.sources[0].url").isNotEmpty());
    }

    @Test
    void shouldPerformAuthenticatedSearchAndGenerateWithCitations() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Synthesizer")
                .lastName("User")
                .email("synth-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        AiSearchGenerateRequest req = AiSearchGenerateRequest.builder()
                .query("What are the key advancements in AI inference latency?")
                .model("gemini-2.0-flash")
                .maxResults(3)
                .build();

        mockMvc.perform(post("/api/v1/ai/search/generate")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").isNotEmpty())
                .andExpect(jsonPath("$.data.sources").isArray())
                .andExpect(jsonPath("$.data.citations").isArray())
                .andExpect(jsonPath("$.data.quotaUsage.used").value(1))
                .andExpect(jsonPath("$.data.quotaUsage.remaining").value(49));
    }

    @Test
    void shouldRejectSearchWhenQuotaExhaustedWith429() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Exhausted")
                .lastName("Searcher")
                .email("exhausted-search-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();
        // Exhaust 50 free credits
        usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 50L, "Exhaust free quota");

        AiSearchGenerateRequest req = AiSearchGenerateRequest.builder()
                .query("Search when out of quota")
                .build();

        mockMvc.perform(post("/api/v1/ai/search/generate")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("QUOTA_EXCEEDED"));
    }
}

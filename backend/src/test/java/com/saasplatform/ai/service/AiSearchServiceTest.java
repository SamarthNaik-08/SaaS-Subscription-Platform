package com.saasplatform.ai.service;

import com.saasplatform.ai.search.dto.AiSearchGenerateRequest;
import com.saasplatform.ai.search.dto.AiSearchGenerateResponse;
import com.saasplatform.ai.search.dto.WebSearchResult;
import com.saasplatform.ai.search.service.WebSearchService;
import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.auth.service.AuthService;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.exception.QuotaExceededException;
import com.saasplatform.usage.service.UsageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiSearchServiceTest {

    @Autowired
    private WebSearchService webSearchService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsageService usageService;

    @Test
    void shouldSearchAndNormalizeSources() {
        WebSearchResult result = webSearchService.search("Quantum computing milestones", 4);

        assertNotNull(result);
        assertEquals("Quantum computing milestones", result.getQuery());
        assertEquals(4, result.getSources().size());
        assertEquals("S1", result.getSources().get(0).getId());
        assertTrue(result.getSources().get(0).getUrl().startsWith("http"));
        assertNotNull(result.getSources().get(0).getTitle());
    }

    @Test
    void shouldSearchAndSynthesizeWithQuotaDeduction() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Search")
                .lastName("ServiceTester")
                .email("search-service-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        AiSearchGenerateRequest req = AiSearchGenerateRequest.builder()
                .query("Latest breakthroughs in transformer architecture")
                .model("gemini-2.0-flash")
                .maxResults(3)
                .build();

        AiSearchGenerateResponse response = webSearchService.searchAndSynthesize(userId, req);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertFalse(response.getSources().isEmpty());
        assertFalse(response.getCitations().isEmpty());
        assertEquals(1L, response.getQuotaUsage().getUsed());
        assertEquals(49L, response.getQuotaUsage().getRemaining());
    }

    @Test
    void shouldRejectSearchWhenQuotaExhausted() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Exhausted")
                .lastName("ServiceTester")
                .email("exhausted-service-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        // Exhaust 50 free credits
        usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 50L, "Exhaust free quota");

        AiSearchGenerateRequest req = AiSearchGenerateRequest.builder()
                .query("Search when limit reached")
                .build();

        assertThrows(QuotaExceededException.class, () ->
                webSearchService.searchAndSynthesize(userId, req)
        );
    }
}

package com.saasplatform.ai.service;

import com.saasplatform.ai.research.dto.DeepResearchRequest;
import com.saasplatform.ai.research.dto.DeepResearchResponse;
import com.saasplatform.ai.research.dto.ResearchSource;
import com.saasplatform.ai.research.service.DeepResearchService;
import com.saasplatform.ai.research.service.ResearchCitationValidator;
import com.saasplatform.ai.research.service.ResearchSourceEvaluator;
import com.saasplatform.ai.search.dto.WebSearchSource;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiDeepResearchServiceTest {

    @Autowired
    private DeepResearchService deepResearchService;

    @Autowired
    private ResearchSourceEvaluator researchSourceEvaluator;

    @Autowired
    private ResearchCitationValidator researchCitationValidator;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsageService usageService;

    @Test
    void shouldDeduplicateSourcesAndEvaluateQuality() {
        WebSearchSource raw1 = WebSearchSource.builder()
                .title("Quantum Computing Foundations")
                .url("https://arxiv.org/abs/2608.11223")
                .snippet("Detailed research on superconducting qubits.")
                .sourceName("arXiv")
                .score(0.88)
                .build();

        // Duplicate of raw1 with trailing slash and query param
        WebSearchSource raw2 = WebSearchSource.builder()
                .title("Quantum Computing Duplicate")
                .url("https://www.arxiv.org/abs/2608.11223/?ref=twitter")
                .snippet("Duplicate snippet.")
                .sourceName("arXiv")
                .score(0.85)
                .build();

        WebSearchSource raw3 = WebSearchSource.builder()
                .title("NIST Quantum Standards")
                .url("https://www.nist.gov/programs-projects/pqc")
                .snippet("Official post-quantum cryptography standards.")
                .sourceName("NIST")
                .score(0.90)
                .build();

        List<ResearchSource> evaluated = researchSourceEvaluator.evaluateAndDeduplicate(
                List.of(raw1, raw2, raw3),
                "Quantum computing",
                10
        );

        assertEquals(2, evaluated.size(), "Should have deduplicated the duplicate arXiv URL");
        assertTrue(evaluated.stream().anyMatch(s -> "Official/Gov".equals(s.getDomainCategory())), "NIST should be categorized as Official/Gov");
        assertTrue(evaluated.stream().anyMatch(s -> "Academic/Research".equals(s.getDomainCategory())), "arXiv should be categorized as Academic/Research");
    }

    @Test
    void shouldValidateCitationsAndStripHallucinatedIds() {
        ResearchSource s1 = ResearchSource.builder().id("S1").title("Paper A").url("https://nature.com/a").build();
        ResearchSource s2 = ResearchSource.builder().id("S2").title("Paper B").url("https://arxiv.org/b").build();

        String report = "Quantum processors achieved 99.9% gate fidelity [S1]. An unverified claim [S999] was also mentioned alongside [S2].";

        List<ResearchSource> validCitations = researchCitationValidator.extractAndValidateCitations(report, List.of(s1, s2));

        assertEquals(2, validCitations.size());
        assertTrue(validCitations.stream().anyMatch(c -> "S1".equals(c.getId())));
        assertTrue(validCitations.stream().anyMatch(c -> "S2".equals(c.getId())));
        assertFalse(validCitations.stream().anyMatch(c -> "S999".equals(c.getId())));

        String sanitized = researchCitationValidator.sanitizeHallucinatedCitations(report, Set.of("S1", "S2"));
        assertTrue(sanitized.contains("[S1]"));
        assertTrue(sanitized.contains("[S2]"));
        assertFalse(sanitized.contains("[S999]"), "Hallucinated citation tag [S999] should be sanitized");
    }

    @Test
    void shouldPerformDeepResearchAndDeductQuota() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Deep")
                .lastName("ServiceTester")
                .email("deep-service-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        DeepResearchRequest req = DeepResearchRequest.builder()
                .topic("Distributed consensus protocols under Byzantine failures")
                .depth(2)
                .maxQueries(3)
                .model("gemini-2.0-flash")
                .build();

        DeepResearchResponse res = deepResearchService.performDeepResearch(userId, req);

        assertNotNull(res);
        assertNotNull(res.getTopic());
        assertNotNull(res.getExecutiveSummary());
        assertFalse(res.getSources().isEmpty());
        assertFalse(res.getCitations().isEmpty());
        assertEquals(1L, res.getQuotaUsage().getUsed());
        assertEquals(49L, res.getQuotaUsage().getRemaining());
    }

    @Test
    void shouldRejectResearchWhenQuotaExhausted() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Exhausted")
                .lastName("ServiceTester")
                .email("exhausted-service-research-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        // Exhaust all 50 free requests
        usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 50L, "Exhaust quota");

        DeepResearchRequest req = DeepResearchRequest.builder()
                .topic("Research topic when out of quota")
                .build();

        assertThrows(QuotaExceededException.class, () ->
                deepResearchService.performDeepResearch(userId, req)
        );
    }
}

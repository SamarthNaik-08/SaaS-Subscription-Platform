package com.saasplatform.ai.research.service;

import com.saasplatform.ai.provider.AiProvider;
import com.saasplatform.ai.provider.AiProviderFactory;
import com.saasplatform.ai.research.dto.*;
import com.saasplatform.ai.search.dto.WebSearchResult;
import com.saasplatform.ai.search.dto.WebSearchSource;
import com.saasplatform.ai.search.provider.WebSearchProvider;
import com.saasplatform.ai.search.provider.WebSearchProviderFactory;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.usage.dto.MetricUsageDto;
import com.saasplatform.usage.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepResearchService {

    private final ResearchPlanner researchPlanner;
    private final ResearchSourceEvaluator researchSourceEvaluator;
    private final ResearchCitationValidator researchCitationValidator;
    private final WebSearchProviderFactory webSearchProviderFactory;
    private final AiProviderFactory aiProviderFactory;
    private final UsageService usageService;

    @Value("${app.ai.deep-research.max-sources:30}")
    private int maxSourcesConfig;

    @Transactional
    public DeepResearchResponse performDeepResearch(UUID userId, DeepResearchRequest request) {
        String topic = request.getTopic() != null ? request.getTopic().trim() : "";
        if (topic.isBlank()) {
            throw new BadRequestException("Research topic cannot be blank");
        }
        if (topic.length() > 2000) {
            throw new BadRequestException("Research topic cannot exceed 2000 characters");
        }

        int depth = Math.min(Math.max(1, request.getDepth()), 2);
        int maxQueries = Math.min(Math.max(1, request.getMaxQueries()), 8);
        String model = (request.getModel() != null && !request.getModel().isBlank())
                ? request.getModel() : "gemini-2.0-flash";

        String topicPreview = topic.length() > 50 ? topic.substring(0, 50) + "..." : topic;

        // 1. Pessimistic Quota Check and Atomic Deduction
        MetricUsageDto usage = usageService.recordUsage(
                userId,
                UsageMetric.AI_REQUEST,
                1,
                "Deep Research Pipeline: " + topicPreview
        );

        // 2. Formulate Structured Multi-Angle Research Plan
        ResearchPlan plan = researchPlanner.createResearchPlan(topic, depth, maxQueries);
        WebSearchProvider searchProvider = webSearchProviderFactory.getProvider();

        List<WebSearchSource> accumulatedRawSources = new ArrayList<>();
        int executedQueryCount = 0;

        // 3. Execute Primary Research Queries
        for (ResearchQuery rq : plan.getPrimaryQueries()) {
            try {
                WebSearchResult res = searchProvider.search(rq.getQueryText(), 5);
                if (res != null && res.getSources() != null) {
                    accumulatedRawSources.addAll(res.getSources());
                }
                executedQueryCount++;
            } catch (Exception e) {
                log.warn("[DeepResearch] Primary query search failed for '{}': {}", rq.getQueryText(), e.getMessage());
            }
        }

        // 4. Execute Follow-Up In-Depth Queries (if depth >= 2)
        if (depth >= 2) {
            for (ResearchQuery fq : plan.getFollowUpQueries()) {
                try {
                    WebSearchResult res = searchProvider.search(fq.getQueryText(), 5);
                    if (res != null && res.getSources() != null) {
                        accumulatedRawSources.addAll(res.getSources());
                    }
                    executedQueryCount++;
                } catch (Exception e) {
                    log.warn("[DeepResearch] Follow-up query search failed for '{}': {}", fq.getQueryText(), e.getMessage());
                }
            }
        }

        // 5. Evaluate, Deduplicate, and Rank Sources by Authority
        List<ResearchSource> verifiedSources = researchSourceEvaluator.evaluateAndDeduplicate(
                accumulatedRawSources,
                topic,
                maxSourcesConfig
        );

        // 6. Handle No-Source or Zero-Evidence Scenario Gracefully
        if (verifiedSources.isEmpty()) {
            return DeepResearchResponse.builder()
                    .topic(topic)
                    .executiveSummary("Insufficient empirical evidence could be retrieved from search indexes to construct an authoritative report.")
                    .keyFindings(List.of("No verifiable authoritative sources found for this inquiry."))
                    .detailedAnalysis("Search queries did not return grounded technical documentation. Please refine the research parameters or keywords.")
                    .conclusion("Inquiry unverified due to lack of indexing data.")
                    .citations(Collections.emptyList())
                    .sources(Collections.emptyList())
                    .plan(plan)
                    .model(model)
                    .provider("None")
                    .searchProvider(searchProvider.getProviderName())
                    .totalQueriesExecuted(executedQueryCount)
                    .promptTokens(10)
                    .completionTokens(30)
                    .totalTokens(40)
                    .quotaUsage(usage)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // 7. Build Grounded Research Prompt with Structured Evidence
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("""
                You are a principal research scientist and technical strategist. Synthesize a comprehensive, authoritative, multi-section Deep Research Report based strictly on the verified empirical sources provided below.

                CRITICAL REQUIREMENTS:
                1. Attribute factual assertions with citation brackets [S1], [S2], etc., matching the provided source IDs.
                2. Explicitly distinguish established empirical facts from analytical inferences.
                3. If sources contain contradictions or conflicting data points, explicitly highlight and contrast the divergence under Contradictions.
                4. DO NOT invent URLs, source names, or hallucinated citation IDs.
                5. Structure your output clearly using the following markdown headers:
                   # EXECUTIVE SUMMARY
                   # KEY FINDINGS (Bullet list)
                   # DETAILED ANALYSIS (Deep technical breakdown with subsections)
                   # CONTRADICTIONS & DISCREPANCIES (Contrasting viewpoints if any)
                   # LIMITATIONS & STRATEGIC OUTLOOK
                   # CONCLUSION

                --- VERIFIED EMPIRICAL RESEARCH SOURCES ---
                """);

        for (ResearchSource src : verifiedSources) {
            promptBuilder.append(String.format("[%s] Title: %s\nSource / Publisher: %s (%s)\nCategory: %s | Authority Score: %.2f\nSnippet: %s\n\n",
                    src.getId(), src.getTitle(), src.getSourceName(), src.getUrl(),
                    src.getDomainCategory(), src.getRelevanceScore(), src.getSnippet()));
        }

        promptBuilder.append("--- END OF VERIFIED SOURCES ---\n\n");
        promptBuilder.append("RESEARCH INQUIRY TOPIC: ").append(topic);

        // 8. Invoke AI Provider for Grounded Synthesis
        Map<String, Object> options = request.getParameters() != null ? new HashMap<>(request.getParameters()) : new HashMap<>();
        if (request.getSystemInstruction() != null) {
            options.put("systemInstruction", request.getSystemInstruction());
        }
        if (request.getTemperature() != null) {
            options.put("temperature", request.getTemperature());
        }

        AiProvider aiProvider = aiProviderFactory.getProvider(model);
        String synthesizedReport = aiProvider.generateText(promptBuilder.toString(), model, options);

        // 9. Citation Verification & Hallucination Removal
        List<ResearchSource> validCitations = researchCitationValidator.extractAndValidateCitations(
                synthesizedReport,
                verifiedSources
        );

        Set<String> validIds = validCitations.stream().map(ResearchSource::getId).collect(Collectors.toSet());
        String sanitizedReport = researchCitationValidator.sanitizeHallucinatedCitations(synthesizedReport, validIds);

        // 10. Extract Structured Sections
        String executiveSummary = extractSection(sanitizedReport, "EXECUTIVE SUMMARY", "KEY FINDINGS");
        List<String> keyFindings = extractBulletPoints(extractSection(sanitizedReport, "KEY FINDINGS", "DETAILED ANALYSIS"));
        String detailedAnalysis = extractSection(sanitizedReport, "DETAILED ANALYSIS", "CONTRADICTIONS");
        String contradictions = extractSection(sanitizedReport, "CONTRADICTIONS", "LIMITATIONS");
        String limitations = extractSection(sanitizedReport, "LIMITATIONS", "CONCLUSION");
        String conclusion = extractSection(sanitizedReport, "CONCLUSION", null);

        // If extraction fell back, provide clean defaults
        if (executiveSummary.isBlank()) {
            executiveSummary = sanitizedReport.length() > 500 ? sanitizedReport.substring(0, 500) + "..." : sanitizedReport;
        }
        if (detailedAnalysis.isBlank()) {
            detailedAnalysis = sanitizedReport;
        }

        List<ResearchSection> sections = new ArrayList<>();
        sections.add(ResearchSection.builder().title("Executive Summary").content(executiveSummary).build());
        if (!detailedAnalysis.isBlank()) {
            sections.add(ResearchSection.builder().title("Technical Analysis").content(detailedAnalysis).build());
        }
        if (!limitations.isBlank()) {
            sections.add(ResearchSection.builder().title("Limitations & Risks").content(limitations).build());
        }

        long promptTokens = Math.max(1, promptBuilder.length() / 4);
        long completionTokens = Math.max(1, sanitizedReport.length() / 4);

        log.info("[DeepResearch] Pipeline complete: userId={}, topic='{}', queries={}, sources={}, citations={}",
                userId, topic, executedQueryCount, verifiedSources.size(), validCitations.size());

        return DeepResearchResponse.builder()
                .topic(topic)
                .executiveSummary(executiveSummary)
                .keyFindings(keyFindings)
                .detailedAnalysis(detailedAnalysis)
                .sections(sections)
                .contradictions(contradictions)
                .limitations(limitations)
                .conclusion(conclusion)
                .citations(validCitations)
                .sources(verifiedSources)
                .plan(plan)
                .model(model)
                .provider(aiProvider.getProviderName())
                .searchProvider(searchProvider.getProviderName())
                .totalQueriesExecuted(executedQueryCount)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .quotaUsage(usage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String extractSection(String text, String startHeader, String endHeader) {
        if (text == null) return "";
        try {
            int startIdx = -1;
            Pattern startPattern = Pattern.compile("#*\\s*" + Pattern.quote(startHeader), Pattern.CASE_INSENSITIVE);
            Matcher startMatcher = startPattern.matcher(text);
            if (startMatcher.find()) {
                startIdx = startMatcher.end();
            }

            if (startIdx == -1) return "";

            int endIdx = text.length();
            if (endHeader != null) {
                Pattern endPattern = Pattern.compile("#*\\s*" + Pattern.quote(endHeader), Pattern.CASE_INSENSITIVE);
                Matcher endMatcher = endPattern.matcher(text);
                if (endMatcher.find(startIdx)) {
                    endIdx = endMatcher.start();
                }
            }

            return text.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private List<String> extractBulletPoints(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        List<String> points = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("*") || trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.matches("^\\d+\\..*")) {
                String clean = trimmed.replaceFirst("^[*\\-•\\d.]+\\s*", "").trim();
                if (!clean.isBlank()) {
                    points.add(clean);
                }
            }
        }
        return points;
    }
}


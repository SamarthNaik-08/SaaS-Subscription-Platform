package com.saasplatform.ai.search.service;

import com.saasplatform.ai.provider.AiProvider;
import com.saasplatform.ai.provider.AiProviderFactory;
import com.saasplatform.ai.search.dto.*;
import com.saasplatform.ai.search.provider.WebSearchProvider;
import com.saasplatform.ai.search.provider.WebSearchProviderFactory;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.usage.dto.MetricUsageDto;
import com.saasplatform.usage.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class WebSearchService {

    private final WebSearchProviderFactory webSearchProviderFactory;
    private final AiProviderFactory aiProviderFactory;
    private final UsageService usageService;

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(S\\d+)\\]");

    public WebSearchResult search(String query, int maxResults) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Search query cannot be blank");
        }
        if (query.length() > 2000) {
            throw new BadRequestException("Search query cannot exceed 2000 characters");
        }
        int boundedMax = Math.min(Math.max(1, maxResults), 10);
        WebSearchProvider provider = webSearchProviderFactory.getProvider();
        return provider.search(query.trim(), boundedMax);
    }

    @Transactional
    public AiSearchGenerateResponse searchAndSynthesize(UUID userId, AiSearchGenerateRequest request) {
        String query = request.getQuery() != null ? request.getQuery().trim() : "";
        if (query.isBlank()) {
            throw new BadRequestException("Search query cannot be blank");
        }
        if (query.length() > 2000) {
            throw new BadRequestException("Search query cannot exceed 2000 characters");
        }

        int maxResults = Math.min(Math.max(1, request.getMaxResults()), 10);
        String model = (request.getModel() != null && !request.getModel().isBlank())
                ? request.getModel() : "gemini-2.0-flash";

        String promptPreview = query.length() > 50 ? query.substring(0, 50) + "..." : query;

        // 1. Check and record quota ATOMICALLY before executing search & synthesis
        MetricUsageDto usage = usageService.recordUsage(
                userId,
                UsageMetric.AI_REQUEST,
                1,
                "Web Search & Synthesis: " + promptPreview
        );

        // 2. Execute Real Web Search Provider
        WebSearchProvider searchProvider = webSearchProviderFactory.getProvider();
        WebSearchResult searchResult = searchProvider.search(query, maxResults);
        List<WebSearchSource> allSources = searchResult.getSources() != null 
                ? searchResult.getSources() 
                : Collections.emptyList();

        if (allSources.isEmpty()) {
            return AiSearchGenerateResponse.builder()
                    .answer("I couldn't find reliable real-time sources for this query. Please try rephrasing your search.")
                    .citations(Collections.emptyList())
                    .sources(Collections.emptyList())
                    .query(query)
                    .model(model)
                    .provider("None")
                    .searchProvider(searchProvider.getProviderName())
                    .promptTokens(10)
                    .completionTokens(20)
                    .totalTokens(30)
                    .quotaUsage(usage)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // 3. Build Structured Prompt with Verified Source Context
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("""
                You are an advanced AI research assistant. Synthesize a comprehensive, accurate, and up-to-date response based strictly on the verified search sources provided below.

                RULES:
                1. Attribute factual statements by appending the citation bracket directly after the sentence (e.g. "...industry growth was reported at 24% [S1].").
                2. Use ONLY the source citation tags provided ([S1], [S2], etc.).
                3. DO NOT invent URLs, domain names, or fake citation IDs.
                4. Distinguish established facts from analytical inferences.

                --- VERIFIED WEB SEARCH SOURCES ---
                """);

        for (WebSearchSource source : allSources) {
            promptBuilder.append(String.format("[%s] Title: %s\nSource: %s\nSnippet: %s\n\n",
                    source.getId(), source.getTitle(), source.getSourceName(), source.getSnippet()));
        }

        promptBuilder.append("--- END OF SOURCES ---\n\n");
        promptBuilder.append("USER INQUIRY: ").append(query);

        // 4. Invoke AI Generation Provider
        Map<String, Object> options = request.getParameters() != null 
                ? new HashMap<>(request.getParameters()) 
                : new HashMap<>();
        if (request.getSystemInstruction() != null) {
            options.put("systemInstruction", request.getSystemInstruction());
        }
        if (request.getTemperature() != null) {
            options.put("temperature", request.getTemperature());
        }

        AiProvider aiProvider = aiProviderFactory.getProvider(model);
        String synthesizedAnswer = aiProvider.generateText(promptBuilder.toString(), model, options);

        // 5. Authoritative Citation Validation & Filtering
        Map<String, WebSearchSource> sourceMap = allSources.stream()
                .collect(Collectors.toMap(WebSearchSource::getId, s -> s, (a, b) -> a));

        Set<String> citedIds = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(synthesizedAnswer);
        while (matcher.find()) {
            String id = matcher.group(1);
            if (sourceMap.containsKey(id)) {
                citedIds.add(id);
            }
        }

        List<WebSearchSource> validatedCitations = citedIds.stream()
                .map(sourceMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // If no explicit tags were generated, provide top relevant sources as citations
        if (validatedCitations.isEmpty() && !allSources.isEmpty()) {
            validatedCitations.add(allSources.get(0));
        }

        long promptTokens = Math.max(1, promptBuilder.length() / 4);
        long completionTokens = Math.max(1, synthesizedAnswer.length() / 4);

        log.info("Web search synthesis completed for userId={}, sources={}, citations={}, promptTokens={}, completionTokens={}",
                userId, allSources.size(), validatedCitations.size(), promptTokens, completionTokens);

        return AiSearchGenerateResponse.builder()
                .answer(synthesizedAnswer)
                .citations(validatedCitations)
                .sources(allSources)
                .query(query)
                .model(model)
                .provider(aiProvider.getProviderName())
                .searchProvider(searchProvider.getProviderName())
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .quotaUsage(usage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

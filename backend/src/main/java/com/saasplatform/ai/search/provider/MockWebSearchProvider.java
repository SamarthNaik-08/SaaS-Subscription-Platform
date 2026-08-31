package com.saasplatform.ai.search.provider;

import com.saasplatform.ai.search.dto.WebSearchResult;
import com.saasplatform.ai.search.dto.WebSearchSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("mockWebSearchProvider")
public class MockWebSearchProvider implements WebSearchProvider {

    @Override
    public String getProviderName() {
        return "Nexus Mock Search Engine";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public WebSearchResult search(String query, int maxResults) {
        log.info("[Mock Web Search] Executing deterministic search for query: '{}', maxResults: {}", query, maxResults);

        int count = Math.min(Math.max(1, maxResults), 10);
        List<WebSearchSource> sources = new ArrayList<>();

        String cleanQuery = query != null ? query.trim() : "artificial intelligence";

        // Deterministic realistic sources based on query
        sources.add(WebSearchSource.builder()
                .id("S1")
                .title("Latest Developments and Industry Analysis: " + cleanQuery)
                .url("https://www.reuters.com/technology/latest-ai-developments-overview")
                .sourceName("Reuters")
                .snippet("Comprehensive report on " + cleanQuery + " focusing on strategic enterprise deployments, performance breakthroughs, and next-generation model benchmarks.")
                .publishedDate("2026-08-30")
                .score(0.96)
                .build());

        if (count >= 2) {
            sources.add(WebSearchSource.builder()
                    .id("S2")
                    .title("Architectural Insights and Market Trends in " + cleanQuery)
                    .url("https://techcrunch.com/2026/08/emerging-trends-deep-dive")
                    .sourceName("TechCrunch")
                    .snippet("Deep dive exploring key market dynamics, real-time inference latency optimizations, and emerging developer adoption patterns.")
                    .publishedDate("2026-08-29")
                    .score(0.91)
                    .build());
        }

        if (count >= 3) {
            sources.add(WebSearchSource.builder()
                    .id("S3")
                    .title("Technical Specification & Benchmark Evaluation on " + cleanQuery)
                    .url("https://arxiv.org/abs/2608.10921")
                    .sourceName("arXiv")
                    .snippet("Empirical evaluation metrics, parameter scaling dynamics, and structured empirical findings regarding " + cleanQuery + ".")
                    .publishedDate("2026-08-28")
                    .score(0.87)
                    .build());
        }

        if (count >= 4) {
            sources.add(WebSearchSource.builder()
                    .id("S4")
                    .title("Global Standards and Security Frameworks Overview")
                    .url("https://www.nature.com/articles/s41586-026-00412-x")
                    .sourceName("Nature")
                    .snippet("Peer-reviewed governance protocols, security verification frameworks, and deterministic validation criteria for automated systems.")
                    .publishedDate("2026-08-25")
                    .score(0.82)
                    .build());
        }

        if (count >= 5) {
            sources.add(WebSearchSource.builder()
                    .id("S5")
                    .title("Practical Engineering Patterns and Case Studies")
                    .url("https://github.blog/2026-08-practical-ai-patterns")
                    .sourceName("GitHub Blog")
                    .snippet("Production engineering case studies demonstrating scalable architectural patterns, high-throughput pipelines, and real-time observability.")
                    .publishedDate("2026-08-22")
                    .score(0.78)
                    .build());
        }

        return WebSearchResult.builder()
                .query(cleanQuery)
                .sources(sources)
                .searchedAt(LocalDateTime.now())
                .provider(getProviderName())
                .build();
    }
}

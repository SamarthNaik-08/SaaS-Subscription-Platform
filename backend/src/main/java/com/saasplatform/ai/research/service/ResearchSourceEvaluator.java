package com.saasplatform.ai.research.service;

import com.saasplatform.ai.research.dto.ResearchSource;
import com.saasplatform.ai.search.dto.WebSearchSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;

@Slf4j
@Component
public class ResearchSourceEvaluator {

    public List<ResearchSource> evaluateAndDeduplicate(List<WebSearchSource> rawSources, String originatingQuery, int maxAllowedSources) {
        if (rawSources == null || rawSources.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, ResearchSource> uniqueMap = new LinkedHashMap<>();
        int sourceCounter = 1;

        for (WebSearchSource raw : rawSources) {
            if (raw.getUrl() == null || raw.getUrl().isBlank()) {
                continue;
            }

            String url = raw.getUrl().trim();
            // Validate scheme: only http and https
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                continue;
            }

            String normalizedUrl = normalizeUrl(url);
            if (uniqueMap.containsKey(normalizedUrl)) {
                continue; // Deduplicate identical URLs
            }

            String domain = extractDomain(url);
            String category = categorizeDomain(domain);
            double baseScore = raw.getScore() != null ? raw.getScore() : 0.80;
            double adjustedScore = adjustScoreByCategory(baseScore, category);

            ResearchSource source = ResearchSource.builder()
                    .id("S" + sourceCounter)
                    .title(raw.getTitle() != null && !raw.getTitle().isBlank() ? raw.getTitle().trim() : "Source " + sourceCounter)
                    .url(url)
                    .normalizedUrl(normalizedUrl)
                    .snippet(raw.getSnippet() != null ? raw.getSnippet().trim() : "")
                    .sourceName(raw.getSourceName() != null && !raw.getSourceName().isBlank() ? raw.getSourceName().trim() : domain)
                    .publishedDate(raw.getPublishedDate())
                    .relevanceScore(Math.min(1.0, Math.round(adjustedScore * 100.0) / 100.0))
                    .originatingQuery(originatingQuery)
                    .domainCategory(category)
                    .build();

            uniqueMap.put(normalizedUrl, source);
            sourceCounter++;

            if (uniqueMap.size() >= maxAllowedSources) {
                break;
            }
        }

        // Sort by evaluated relevance score descending
        List<ResearchSource> result = new ArrayList<>(uniqueMap.values());
        result.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

        // Re-index citation IDs based on quality ranking: S1 is the highest ranked source
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setId("S" + (i + 1));
        }

        log.info("[ResearchSourceEvaluator] Normalized and ranked {} authoritative sources from {} raw search items",
                result.size(), rawSources.size());

        return result;
    }

    public String normalizeUrl(String urlString) {
        try {
            URI uri = new URI(urlString);
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            String path = uri.getPath() != null ? uri.getPath() : "";
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }
            return host + path;
        } catch (Exception e) {
            return urlString.toLowerCase().replace("https://", "").replace("http://", "").replace("www.", "");
        }
    }

    private String extractDomain(String urlString) {
        try {
            URI uri = new URI(urlString);
            String host = uri.getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {}
        return "Web Resource";
    }

    private String categorizeDomain(String domain) {
        String d = domain.toLowerCase();
        if (d.endsWith(".edu") || d.contains("arxiv.org") || d.contains("nature.com") ||
                d.contains("sciencedirect.com") || d.contains("ieee.org") || d.contains("acm.org") || d.contains("biorxiv.org")) {
            return "Academic/Research";
        }
        if (d.endsWith(".gov") || d.endsWith(".mil") || d.contains("who.int") || d.contains("nist.gov") || d.contains("europa.eu")) {
            return "Official/Gov";
        }
        if (d.contains("reuters.com") || d.contains("bloomberg.com") || d.contains("wsj.com") ||
                d.contains("ft.com") || d.contains("bbc.com") || d.contains("techcrunch.com") ||
                d.contains("theverge.com") || d.contains("wired.com") || d.contains("technologyreview.com")) {
            return "News/Journalism";
        }
        if (d.contains("github.com") || d.contains("huggingface.co") || d.contains("apache.org") ||
                d.contains("openai.com") || d.contains("anthropic.com") || d.contains("google.com") ||
                d.contains("microsoft.com") || d.contains("meta.com") || d.contains("aws.amazon.com")) {
            return "Industry/Tech";
        }
        return "General Web";
    }

    private double adjustScoreByCategory(double baseScore, String category) {
        return switch (category) {
            case "Official/Gov" -> baseScore + 0.15;
            case "Academic/Research" -> baseScore + 0.12;
            case "News/Journalism" -> baseScore + 0.08;
            case "Industry/Tech" -> baseScore + 0.05;
            default -> baseScore;
        };
    }
}

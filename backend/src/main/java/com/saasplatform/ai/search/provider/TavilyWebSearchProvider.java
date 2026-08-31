package com.saasplatform.ai.search.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.ai.search.dto.WebSearchResult;
import com.saasplatform.ai.search.dto.WebSearchSource;
import com.saasplatform.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("tavilyWebSearchProvider")
public class TavilyWebSearchProvider implements WebSearchProvider {

    @Value("${app.ai.web-search.api-key:}")
    private String apiKey;

    @Value("${app.ai.web-search.base-url:https://api.tavily.com/search}")
    private String apiUrl;

    @Value("${app.ai.web-search.timeout-ms:10000}")
    private int timeoutMs;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;

    public TavilyWebSearchProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(10000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String getProviderName() {
        return "Tavily Live Search API";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("placeholder") && !apiKey.startsWith("your_");
    }

    @Override
    public WebSearchResult search(String query, int maxResults) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Search query cannot be blank");
        }

        String cleanQuery = query.trim();
        int count = Math.min(Math.max(1, maxResults), 10);

        log.info("[Tavily Search] Executing live search for: '{}', maxResults: {}", cleanQuery, count);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("api_key", apiKey.trim());
            requestBody.put("query", cleanQuery);
            requestBody.put("search_depth", "basic");
            requestBody.put("max_results", count);
            requestBody.put("include_images", false);
            requestBody.put("include_answer", false);

            String responseJson = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode resultsArray = root.path("results");

            List<WebSearchSource> sources = new ArrayList<>();
            if (resultsArray.isArray()) {
                int index = 1;
                for (JsonNode item : resultsArray) {
                    String title = item.path("title").asText("Source " + index);
                    String url = item.path("url").asText("");
                    String content = item.path("content").asText("");
                    double score = item.path("score").asDouble(0.85);
                    String publishedDate = item.path("published_date").asText(null);

                    // Skip empty or unsafe URLs
                    if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                        continue;
                    }

                    String domain = extractDomain(url);

                    sources.add(WebSearchSource.builder()
                            .id("S" + index)
                            .title(title)
                            .url(url)
                            .snippet(content)
                            .sourceName(domain)
                            .publishedDate(publishedDate)
                            .score(score)
                            .build());

                    index++;
                    if (sources.size() >= count) break;
                }
            }

            log.info("[Tavily Search] Retrieved {} live search results for query: '{}'", sources.size(), cleanQuery);

            return WebSearchResult.builder()
                    .query(cleanQuery)
                    .sources(sources)
                    .searchedAt(LocalDateTime.now())
                    .provider(getProviderName())
                    .build();

        } catch (Exception e) {
            log.error("[Tavily Search] Failed to retrieve live search results: {}", e.getMessage(), e);
            throw new RuntimeException("Web search provider error: " + e.getMessage(), e);
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
        return "Web Source";
    }
}

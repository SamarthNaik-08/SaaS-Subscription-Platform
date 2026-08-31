package com.saasplatform.ai.search.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSearchProviderFactory {

    private final TavilyWebSearchProvider tavilyWebSearchProvider;
    private final MockWebSearchProvider mockWebSearchProvider;

    @Value("${app.ai.web-search.provider:auto}")
    private String configuredProvider;

    public WebSearchProvider getProvider() {
        String mode = configuredProvider != null ? configuredProvider.trim().toLowerCase() : "auto";

        if ("mock".equals(mode)) {
            log.info("[WebSearchProviderFactory] Using Mock search provider by configuration");
            return mockWebSearchProvider;
        }

        if ("tavily".equals(mode) || "auto".equals(mode)) {
            if (tavilyWebSearchProvider.isAvailable()) {
                return tavilyWebSearchProvider;
            }
        }

        log.info("[WebSearchProviderFactory] Live provider not available, defaulting to Mock search provider");
        return mockWebSearchProvider;
    }
}

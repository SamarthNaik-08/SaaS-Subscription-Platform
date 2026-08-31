package com.saasplatform.ai.search.provider;

import com.saasplatform.ai.search.dto.WebSearchResult;

public interface WebSearchProvider {

    String getProviderName();

    boolean isAvailable();

    WebSearchResult search(String query, int maxResults);
}

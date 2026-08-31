package com.saasplatform.ai.search.dto;

import com.saasplatform.usage.dto.MetricUsageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSearchGenerateResponse {

    private String answer;

    @Builder.Default
    private List<WebSearchSource> citations = new ArrayList<>();

    @Builder.Default
    private List<WebSearchSource> sources = new ArrayList<>();

    private String query;

    private String model;

    private String provider; // AI generation provider (e.g. Gemini / OpenAI)

    private String searchProvider; // Web search provider (e.g. Tavily / Mock)

    private long promptTokens;

    private long completionTokens;

    private long totalTokens;

    private MetricUsageDto quotaUsage;

    private LocalDateTime timestamp;
}

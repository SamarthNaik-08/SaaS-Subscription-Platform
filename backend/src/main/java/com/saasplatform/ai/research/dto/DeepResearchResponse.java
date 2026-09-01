package com.saasplatform.ai.research.dto;

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
public class DeepResearchResponse {

    private String topic;

    private String executiveSummary;

    @Builder.Default
    private List<String> keyFindings = new ArrayList<>();

    private String detailedAnalysis;

    @Builder.Default
    private List<ResearchSection> sections = new ArrayList<>();

    private String contradictions;

    private String limitations;

    private String conclusion;

    @Builder.Default
    private List<ResearchSource> citations = new ArrayList<>();

    @Builder.Default
    private List<ResearchSource> sources = new ArrayList<>();

    private ResearchPlan plan;

    private String model;

    private String provider; // AI provider (e.g. Gemini / OpenAI)

    private String searchProvider; // Search provider (e.g. Tavily / Mock)

    private int totalQueriesExecuted;

    private long promptTokens;

    private long completionTokens;

    private long totalTokens;

    private MetricUsageDto quotaUsage;

    private LocalDateTime timestamp;
}

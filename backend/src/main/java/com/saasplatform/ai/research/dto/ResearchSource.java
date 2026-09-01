package com.saasplatform.ai.research.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchSource {

    private String id; // e.g. "S1", "S2"

    private String title;

    private String url;

    private String normalizedUrl;

    private String snippet;

    private String sourceName; // e.g. "Reuters", "Nature", "arXiv"

    private String publishedDate;

    private Double relevanceScore; // 0.0 to 1.0

    private String originatingQuery;

    private String domainCategory; // "Academic/Research", "News/Journalism", "Official/Gov", "Industry/Tech", "Web"
}

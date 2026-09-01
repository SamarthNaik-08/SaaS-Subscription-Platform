package com.saasplatform.ai.research.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchPlan {

    private String topic;

    @Builder.Default
    private List<ResearchQuery> primaryQueries = new ArrayList<>();

    @Builder.Default
    private List<ResearchQuery> followUpQueries = new ArrayList<>();

    @Builder.Default
    private List<String> targetFocusAreas = new ArrayList<>();
}

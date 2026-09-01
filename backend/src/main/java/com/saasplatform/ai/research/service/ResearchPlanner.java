package com.saasplatform.ai.research.service;

import com.saasplatform.ai.research.dto.ResearchPlan;
import com.saasplatform.ai.research.dto.ResearchQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ResearchPlanner {

    public ResearchPlan createResearchPlan(String topic, int depth, int maxQueries) {
        String cleanTopic = topic != null ? topic.trim() : "artificial intelligence";
        int totalLimit = Math.min(Math.max(1, maxQueries), 8);
        int targetDepth = Math.min(Math.max(1, depth), 2);

        log.info("[ResearchPlanner] Formulating research plan for: '{}', depth: {}, maxQueries: {}", cleanTopic, targetDepth, totalLimit);

        List<ResearchQuery> primaryQueries = new ArrayList<>();
        List<ResearchQuery> followUpQueries = new ArrayList<>();
        List<String> focusAreas = new ArrayList<>();

        // Primary Query 1: Comprehensive state and overview
        primaryQueries.add(ResearchQuery.builder()
                .queryText(cleanTopic + " comprehensive overview state of the art analysis")
                .focusArea("Executive Landscape")
                .depthLevel(1)
                .build());
        focusAreas.add("Executive Landscape");

        // Primary Query 2: Technical architecture and benchmarks
        if (primaryQueries.size() < totalLimit) {
            primaryQueries.add(ResearchQuery.builder()
                    .queryText(cleanTopic + " technical benchmarks architecture performance evaluation")
                    .focusArea("Technical Evaluation & Benchmarks")
                    .depthLevel(1)
                    .build());
            focusAreas.add("Technical Evaluation & Benchmarks");
        }

        // Primary Query 3: Real-world adoption, production case studies, challenges
        if (primaryQueries.size() < totalLimit) {
            primaryQueries.add(ResearchQuery.builder()
                    .queryText(cleanTopic + " industry adoption case studies practical implementation challenges")
                    .focusArea("Industry Implementation & Case Studies")
                    .depthLevel(1)
                    .build());
            focusAreas.add("Industry Implementation & Case Studies");
        }

        // Depth 2: Follow-up queries for deeper synthesis if depth >= 2
        if (targetDepth >= 2 && (primaryQueries.size() + followUpQueries.size()) < totalLimit) {
            followUpQueries.add(ResearchQuery.builder()
                    .queryText(cleanTopic + " security vulnerabilities governance ethical and legal frameworks")
                    .focusArea("Security, Governance & Risks")
                    .depthLevel(2)
                    .build());
            focusAreas.add("Security, Governance & Risks");
        }

        if (targetDepth >= 2 && (primaryQueries.size() + followUpQueries.size()) < totalLimit) {
            followUpQueries.add(ResearchQuery.builder()
                    .queryText(cleanTopic + " future trends market outlook emerging breakthroughs")
                    .focusArea("Future Horizons & Strategic Outlook")
                    .depthLevel(2)
                    .build());
            focusAreas.add("Future Horizons & Strategic Outlook");
        }

        return ResearchPlan.builder()
                .topic(cleanTopic)
                .primaryQueries(primaryQueries)
                .followUpQueries(followUpQueries)
                .targetFocusAreas(focusAreas)
                .build();
    }
}

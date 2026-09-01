package com.saasplatform.ai.research.service;

import com.saasplatform.ai.research.dto.ResearchSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ResearchCitationValidator {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(S\\d+)\\]");

    public List<ResearchSource> extractAndValidateCitations(String reportContent, List<ResearchSource> availableSources) {
        if (reportContent == null || reportContent.isBlank() || availableSources == null || availableSources.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, ResearchSource> sourceMap = availableSources.stream()
                .collect(Collectors.toMap(ResearchSource::getId, s -> s, (a, b) -> a));

        Set<String> citedIds = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(reportContent);

        while (matcher.find()) {
            String citationId = matcher.group(1);
            if (sourceMap.containsKey(citationId)) {
                citedIds.add(citationId);
            } else {
                log.warn("[ResearchCitationValidator] Filtered out unverified/hallucinated citation: [{}]", citationId);
            }
        }

        List<ResearchSource> validCitations = citedIds.stream()
                .map(sourceMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Fallback: If no inline bracket citations were matched, return top relevant sources as primary references
        if (validCitations.isEmpty()) {
            validCitations.addAll(availableSources.stream().limit(Math.min(5, availableSources.size())).toList());
        }

        log.info("[ResearchCitationValidator] Validated {} authoritative citations from {} total candidate sources",
                validCitations.size(), availableSources.size());

        return validCitations;
    }

    public String sanitizeHallucinatedCitations(String reportContent, Set<String> validIds) {
        if (reportContent == null || reportContent.isBlank()) {
            return "";
        }

        Matcher matcher = CITATION_PATTERN.matcher(reportContent);
        StringBuilder sanitized = new StringBuilder();

        while (matcher.find()) {
            String id = matcher.group(1);
            if (validIds.contains(id)) {
                matcher.appendReplacement(sanitized, Matcher.quoteReplacement("[" + id + "]"));
            } else {
                matcher.appendReplacement(sanitized, ""); // Strip unverified citation tag
            }
        }
        matcher.appendTail(sanitized);
        return sanitized.toString();
    }
}

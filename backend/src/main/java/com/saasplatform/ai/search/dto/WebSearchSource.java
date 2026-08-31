package com.saasplatform.ai.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSearchSource {

    private String id; // e.g. "S1", "S2"

    private String title;

    private String url;

    private String snippet;

    private String sourceName; // e.g. "Reuters", "TechCrunch", "Wikipedia"

    private String publishedDate;

    private Double score;
}

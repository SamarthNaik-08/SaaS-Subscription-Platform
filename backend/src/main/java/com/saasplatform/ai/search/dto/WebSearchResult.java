package com.saasplatform.ai.search.dto;

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
public class WebSearchResult {

    private String query;

    @Builder.Default
    private List<WebSearchSource> sources = new ArrayList<>();

    private LocalDateTime searchedAt;

    private String provider;
}

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
public class ResearchSection {

    private String title;

    private String content;

    @Builder.Default
    private List<String> citedSourceIds = new ArrayList<>();
}

package com.saasplatform.ai.research.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchQuery {

    private String queryText;

    private String focusArea;

    private int depthLevel;
}

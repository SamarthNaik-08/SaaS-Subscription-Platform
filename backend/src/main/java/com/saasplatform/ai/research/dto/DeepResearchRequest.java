package com.saasplatform.ai.research.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeepResearchRequest {

    @NotBlank(message = "Research topic cannot be blank")
    @Size(max = 2000, message = "Research topic cannot exceed 2000 characters")
    private String topic;

    @Min(value = 1, message = "Depth must be at least 1")
    @Max(value = 2, message = "Depth cannot exceed 2")
    @Builder.Default
    private int depth = 1;

    @Min(value = 1, message = "maxQueries must be at least 1")
    @Max(value = 8, message = "maxQueries cannot exceed 8")
    @Builder.Default
    private int maxQueries = 4;

    private String model;

    private String systemInstruction;

    private Double temperature;

    private Map<String, Object> parameters;
}

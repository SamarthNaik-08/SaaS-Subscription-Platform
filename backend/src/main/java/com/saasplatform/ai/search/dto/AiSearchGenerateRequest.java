package com.saasplatform.ai.search.dto;

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
public class AiSearchGenerateRequest {

    @NotBlank(message = "Search prompt query cannot be blank")
    @Size(max = 2000, message = "Query cannot exceed 2000 characters")
    private String query;

    private String model;

    @Min(value = 1, message = "maxResults must be at least 1")
    @Max(value = 10, message = "maxResults cannot exceed 10")
    @Builder.Default
    private int maxResults = 5;

    private String systemInstruction;

    private Double temperature;

    private Map<String, Object> parameters;
}

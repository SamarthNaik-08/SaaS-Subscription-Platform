package com.saasplatform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateRequest {

    @NotBlank(message = "Prompt cannot be blank")
    private String prompt;

    private String model;

    private String systemInstruction;

    private Double temperature;

    private Map<String, Object> parameters;
}

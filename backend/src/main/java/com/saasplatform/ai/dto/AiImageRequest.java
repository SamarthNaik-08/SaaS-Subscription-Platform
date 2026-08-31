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
public class AiImageRequest {

    @NotBlank(message = "Prompt cannot be blank")
    private String prompt;

    private String model;

    @Builder.Default
    private String aspectRatio = "1:1";

    private String stylePreset;

    private Map<String, Object> parameters;
}

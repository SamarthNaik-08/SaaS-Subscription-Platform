package com.saasplatform.ai.dto;

import com.saasplatform.usage.dto.MetricUsageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiImageResponse {

    private String imageUrl;

    private String prompt;

    private String revisedPrompt;

    private String model;

    private String provider;

    private String aspectRatio;

    private String stylePreset;

    private MetricUsageDto quotaUsage;

    private LocalDateTime timestamp;
}

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
public class AiGenerateResponse {

    private String text;

    private String model;

    private String provider;

    private long promptTokens;

    private long completionTokens;

    private long totalTokens;

    private MetricUsageDto quotaUsage;

    private LocalDateTime timestamp;
}

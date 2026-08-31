package com.saasplatform.usage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUsageResponse {
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Map<String, MetricUsageDto> metrics;
}

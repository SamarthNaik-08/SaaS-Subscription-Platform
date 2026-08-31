package com.saasplatform.usage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricUsageDto {
    private long used;
    private long limit;
    private long remaining;
    private double percentage;
}

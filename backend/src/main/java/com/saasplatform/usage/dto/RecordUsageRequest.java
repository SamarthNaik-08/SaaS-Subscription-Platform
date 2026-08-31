package com.saasplatform.usage.dto;

import com.saasplatform.common.enums.UsageMetric;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordUsageRequest {

    @NotNull(message = "Metric is required")
    private UsageMetric metric;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Long quantity;

    private String metadata;
}

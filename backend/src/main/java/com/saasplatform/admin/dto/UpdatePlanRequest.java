package com.saasplatform.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlanRequest {

    @NotBlank(message = "Plan name is required")
    private String name;

    private String description;

    @NotNull(message = "Monthly price is required")
    @DecimalMin(value = "0.0", message = "Monthly price must be non-negative")
    private BigDecimal priceMonthly;

    @NotNull(message = "Yearly price is required")
    @DecimalMin(value = "0.0", message = "Yearly price must be non-negative")
    private BigDecimal priceYearly;

    @NotNull(message = "Monthly AI limit is required")
    @Min(value = 0, message = "Monthly AI limit must be non-negative")
    private Integer monthlyAiLimit;

    @NotNull(message = "Storage limit in MB is required")
    @Min(value = 0, message = "Storage limit must be non-negative")
    private Long storageLimitMb;

    private Boolean isActive;
}

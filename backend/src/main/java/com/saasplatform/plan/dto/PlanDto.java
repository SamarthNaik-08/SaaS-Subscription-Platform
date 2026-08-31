package com.saasplatform.plan.dto;

import com.saasplatform.common.enums.PlanCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDto {
    private UUID id;
    private PlanCode code;
    private String name;
    private String description;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private String currency;
    private Integer monthlyAiLimit;
    private Long storageLimitMb;
    private boolean isActive;

    public static PlanDto fromEntity(com.saasplatform.plan.entity.Plan plan) {
        if (plan == null) return null;
        return PlanDto.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .priceMonthly(plan.getPriceMonthly())
                .priceYearly(plan.getPriceYearly())
                .currency(plan.getCurrency())
                .monthlyAiLimit(plan.getMonthlyAiLimit())
                .storageLimitMb(plan.getStorageLimitMb())
                .isActive(plan.isActive())
                .build();
    }
}

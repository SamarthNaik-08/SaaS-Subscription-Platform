package com.saasplatform.billing.dto;

import com.saasplatform.common.enums.BillingInterval;
import com.saasplatform.common.enums.PlanCode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentOrderRequest {

    @NotNull(message = "Plan code is required")
    private PlanCode planCode;

    @NotNull(message = "Billing interval is required (MONTHLY or YEARLY)")
    private BillingInterval billingInterval;
}

package com.saasplatform.billing.dto;

import com.saasplatform.common.enums.PaymentGatewayProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingConfigResponse {

    private PaymentGatewayProvider provider;
    private String keyId;
    private String currency;
}

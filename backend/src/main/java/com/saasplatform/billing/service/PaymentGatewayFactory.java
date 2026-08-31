package com.saasplatform.billing.service;

import com.saasplatform.common.enums.PaymentGatewayProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentGatewayFactory {

    private final RazorpayPaymentGatewayService razorpayService;
    private final SandboxPaymentGatewayService sandboxService;

    @Value("${app.payment.provider:SANDBOX}")
    private String configuredProvider;

    public PaymentGatewayService getPaymentGatewayService() {
        if ("RAZORPAY".equalsIgnoreCase(configuredProvider)) {
            return razorpayService;
        }
        return sandboxService;
    }

    public PaymentGatewayService getServiceByProvider(PaymentGatewayProvider provider) {
        if (provider == PaymentGatewayProvider.RAZORPAY) {
            return razorpayService;
        }
        return sandboxService;
    }
}

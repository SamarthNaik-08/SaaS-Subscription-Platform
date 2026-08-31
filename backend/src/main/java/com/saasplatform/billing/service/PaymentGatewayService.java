package com.saasplatform.billing.service;

import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.common.enums.PaymentGatewayProvider;

public interface PaymentGatewayService {

    String createGatewayOrder(PaymentOrder internalOrder);

    boolean verifyPaymentSignature(String gatewayOrderId, String gatewayPaymentId, String gatewaySignature);

    boolean verifyWebhookSignature(String rawBody, String signatureHeader);

    PaymentGatewayProvider getProvider();

    String getPublicKeyId();
}

package com.saasplatform.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.billing.entity.WebhookEvent;
import com.saasplatform.billing.repository.PaymentOrderRepository;
import com.saasplatform.billing.repository.WebhookEventRepository;
import com.saasplatform.common.enums.PaymentGatewayProvider;
import com.saasplatform.common.enums.PaymentOrderStatus;
import com.saasplatform.common.enums.WebhookEventStatus;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingWebhookTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private PaymentOrderService paymentOrderService;

    @Mock
    private PaymentGatewayFactory paymentGatewayFactory;

    @Mock
    private PaymentGatewayService razorpayGatewayService;

    @Mock
    private PaymentGatewayService sandboxGatewayService;

    @Mock
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private BillingWebhookService billingWebhookService;

    @BeforeEach
    void setUp() {
        billingWebhookService = new BillingWebhookService(
                webhookEventRepository,
                paymentOrderRepository,
                paymentOrderService,
                paymentGatewayFactory,
                objectMapper,
                jwtService
        );
    }

    @Test
    void shouldProcessOrderPaidWebhookSuccessfully() {
        String payload = "{\"event\":\"order.paid\",\"id\":\"evt_12345\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_rzp_99\",\"order_id\":\"order_rzp_88\",\"signature\":\"sig_rzp_77\"}}}}";
        String signature = "valid_sig";

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .gatewayOrderId("order_rzp_88")
                .status(PaymentOrderStatus.CREATED)
                .build();

        when(paymentGatewayFactory.getServiceByProvider(PaymentGatewayProvider.RAZORPAY)).thenReturn(razorpayGatewayService);
        when(razorpayGatewayService.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(razorpayGatewayService.getProvider()).thenReturn(PaymentGatewayProvider.RAZORPAY);
        when(jwtService.hashToken(payload)).thenReturn("payload_hash_12345");
        when(webhookEventRepository.findByProviderAndProviderEventId(PaymentGatewayProvider.RAZORPAY, "evt_12345")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentOrderRepository.findByGatewayOrderId("order_rzp_88")).thenReturn(Optional.of(paymentOrder));

        billingWebhookService.processRazorpayWebhook(payload, signature);

        verify(paymentOrderService, times(1)).settlePayment(paymentOrder, "pay_rzp_99", "sig_rzp_77");
        verify(webhookEventRepository, times(2)).save(any(WebhookEvent.class));
    }

    @Test
    void shouldSkipDuplicateWebhookEventsIdempotently() {
        String payload = "{\"event\":\"order.paid\",\"id\":\"evt_duplicate\",\"payload\":{}}";
        String signature = "valid_sig";

        WebhookEvent existingEvent = WebhookEvent.builder()
                .provider(PaymentGatewayProvider.RAZORPAY)
                .providerEventId("evt_duplicate")
                .status(WebhookEventStatus.PROCESSED)
                .build();

        when(paymentGatewayFactory.getServiceByProvider(PaymentGatewayProvider.RAZORPAY)).thenReturn(razorpayGatewayService);
        when(razorpayGatewayService.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(razorpayGatewayService.getProvider()).thenReturn(PaymentGatewayProvider.RAZORPAY);
        when(jwtService.hashToken(payload)).thenReturn("payload_hash_dup");
        when(webhookEventRepository.findByProviderAndProviderEventId(PaymentGatewayProvider.RAZORPAY, "evt_duplicate")).thenReturn(Optional.of(existingEvent));

        billingWebhookService.processRazorpayWebhook(payload, signature);

        // Verify settlement was NOT called again
        verifyNoInteractions(paymentOrderService);
    }

    @Test
    void shouldRejectInvalidWebhookSignature() {
        String payload = "{\"event\":\"order.paid\"}";
        String signature = "invalid_sig";

        when(paymentGatewayFactory.getServiceByProvider(PaymentGatewayProvider.RAZORPAY)).thenReturn(razorpayGatewayService);
        when(paymentGatewayFactory.getServiceByProvider(PaymentGatewayProvider.SANDBOX)).thenReturn(sandboxGatewayService);
        when(razorpayGatewayService.verifyWebhookSignature(payload, signature)).thenReturn(false);
        when(sandboxGatewayService.verifyWebhookSignature(payload, signature)).thenReturn(false);

        assertThrows(BadRequestException.class, () ->
                billingWebhookService.processRazorpayWebhook(payload, signature));
    }
}

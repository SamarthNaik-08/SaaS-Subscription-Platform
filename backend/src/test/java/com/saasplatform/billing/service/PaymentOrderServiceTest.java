package com.saasplatform.billing.service;

import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.billing.dto.*;
import com.saasplatform.billing.entity.Invoice;
import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.billing.repository.PaymentOrderRepository;
import com.saasplatform.common.enums.*;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.ForbiddenException;
import com.saasplatform.notification.service.NotificationService;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.plan.repository.PlanRepository;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.service.SubscriptionService;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private TaxCalculationService taxCalculationService;

    @Mock
    private PaymentGatewayFactory paymentGatewayFactory;

    @Mock
    private PaymentGatewayService gatewayService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentOrderService paymentOrderService;

    private UUID userId;
    private User user;
    private Plan proPlan;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder().id(userId).email("user@saas.com").firstName("Alice").lastName("User").globalRole(GlobalRole.USER).build();

        proPlan = Plan.builder()
                .id(UUID.randomUUID())
                .code(PlanCode.PRO)
                .name("Pro Plan")
                .priceMonthly(new BigDecimal("499.00"))
                .priceYearly(new BigDecimal("4990.00"))
                .currency("INR")
                .monthlyAiLimit(1000)
                .storageLimitMb(5120L)
                .isActive(true)
                .build();
    }

    @Test
    void shouldCreatePaymentOrderWithAuthoritativeServerCalculatedAmount() {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(PlanCode.PRO, BillingInterval.MONTHLY);

        when(planRepository.findByCode(PlanCode.PRO)).thenReturn(Optional.of(proPlan));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taxCalculationService.calculateTax(new BigDecimal("499.00"))).thenReturn(
                new TaxCalculationResult(new BigDecimal("499.00"), new BigDecimal("18.00"), new BigDecimal("89.82"), new BigDecimal("588.82"))
        );
        when(paymentGatewayFactory.getPaymentGatewayService()).thenReturn(gatewayService);
        when(gatewayService.getProvider()).thenReturn(PaymentGatewayProvider.SANDBOX);
        when(gatewayService.getPublicKeyId()).thenReturn("rzp_sandbox_mockKeyId");
        when(gatewayService.createGatewayOrder(any())).thenReturn("order_sandbox_12345");
        when(paymentOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentOrderDto result = paymentOrderService.createPaymentOrder(userId, request);

        assertNotNull(result);
        assertEquals(PlanCode.PRO, result.getPlanCode());
        assertEquals(new BigDecimal("588.82"), result.getAmount()); // 499 + 18% GST = 588.82
        assertEquals("order_sandbox_12345", result.getGatewayOrderId());
        verify(paymentOrderRepository, times(2)).save(any(PaymentOrder.class));
    }

    @Test
    void shouldRejectPurchasingFreePlan() {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(PlanCode.FREE, BillingInterval.MONTHLY);
        assertThrows(BadRequestException.class, () ->
                paymentOrderService.createPaymentOrder(userId, request));
    }

    @Test
    void shouldVerifyValidPaymentAndUpgradeSubscription() {
        PaymentOrder order = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(proPlan)
                .billingInterval(BillingInterval.MONTHLY)
                .amount(new BigDecimal("588.82"))
                .gatewayProvider(PaymentGatewayProvider.SANDBOX)
                .gatewayOrderId("order_sandbox_12345")
                .status(PaymentOrderStatus.CREATED)
                .build();

        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(proPlan)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("INV-202608-00001")
                .totalAmount(new BigDecimal("588.82"))
                .build();

        when(paymentOrderRepository.findByGatewayOrderId("order_sandbox_12345")).thenReturn(Optional.of(order));
        when(paymentGatewayFactory.getServiceByProvider(PaymentGatewayProvider.SANDBOX)).thenReturn(gatewayService);
        when(gatewayService.verifyPaymentSignature("order_sandbox_12345", "pay_sandbox_1", "sig_valid")).thenReturn(true);
        when(paymentOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(subscriptionService.upgradeSubscription(eq(userId), eq(proPlan), eq(BillingInterval.MONTHLY), any())).thenReturn(subscription);
        when(invoiceService.generateInvoice(eq(user), eq(subscription), any(), eq(proPlan), eq(BillingInterval.MONTHLY))).thenReturn(invoice);

        VerifyPaymentRequest request = new VerifyPaymentRequest("order_sandbox_12345", "pay_sandbox_1", "sig_valid");
        PaymentVerificationResponse response = paymentOrderService.verifyPayment(userId, request);

        assertNotNull(response);
        assertEquals(PaymentOrderStatus.PAID, response.getOrderStatus());
        assertEquals("INV-202608-00001", response.getInvoiceNumber());
        assertEquals(new BigDecimal("588.82"), response.getTotalAmount());
        assertEquals(PaymentOrderStatus.PAID, order.getStatus());
    }

    @Test
    void shouldRejectInvalidPaymentSignature() {
        PaymentOrder order = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(proPlan)
                .billingInterval(BillingInterval.MONTHLY)
                .amount(new BigDecimal("588.82"))
                .gatewayProvider(PaymentGatewayProvider.SANDBOX)
                .gatewayOrderId("order_sandbox_12345")
                .status(PaymentOrderStatus.CREATED)
                .build();

        when(paymentOrderRepository.findByGatewayOrderId("order_sandbox_12345")).thenReturn(Optional.of(order));
        when(paymentGatewayFactory.getServiceByProvider(PaymentGatewayProvider.SANDBOX)).thenReturn(gatewayService);
        when(gatewayService.verifyPaymentSignature("order_sandbox_12345", "pay_sandbox_1", "sig_invalid")).thenReturn(false);

        VerifyPaymentRequest request = new VerifyPaymentRequest("order_sandbox_12345", "pay_sandbox_1", "sig_invalid");
        assertThrows(BadRequestException.class, () ->
                paymentOrderService.verifyPayment(userId, request));

        assertEquals(PaymentOrderStatus.FAILED, order.getStatus());
    }

    @Test
    void shouldRejectVerifyingOrderFromDifferentUser() {
        User anotherUser = User.builder().id(UUID.randomUUID()).build();
        PaymentOrder foreignOrder = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .user(anotherUser)
                .gatewayOrderId("order_sandbox_foreign")
                .status(PaymentOrderStatus.CREATED)
                .build();

        when(paymentOrderRepository.findByGatewayOrderId("order_sandbox_foreign")).thenReturn(Optional.of(foreignOrder));

        VerifyPaymentRequest request = new VerifyPaymentRequest("order_sandbox_foreign", "pay_1", "sig_1");
        assertThrows(ForbiddenException.class, () ->
                paymentOrderService.verifyPayment(userId, request));
    }
}

package com.saasplatform.billing.service;

import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.billing.dto.InvoiceDto;
import com.saasplatform.billing.dto.TaxCalculationResult;
import com.saasplatform.billing.entity.Invoice;
import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.billing.repository.InvoiceRepository;
import com.saasplatform.common.enums.BillingInterval;
import com.saasplatform.common.enums.InvoiceStatus;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceNumberGenerator invoiceNumberGenerator;

    @Mock
    private TaxCalculationService taxCalculationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InvoiceService invoiceService;

    private UUID userId;
    private User user;
    private Plan proPlan;
    private Subscription subscription;
    private PaymentOrder paymentOrder;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder().id(userId).email("user@saas.com").firstName("John").lastName("Doe").build();

        proPlan = Plan.builder()
                .id(UUID.randomUUID())
                .code(PlanCode.PRO)
                .name("Pro Plan")
                .priceMonthly(new BigDecimal("499.00"))
                .priceYearly(new BigDecimal("4990.00"))
                .currency("INR")
                .build();

        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(proPlan)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .build();

        paymentOrder = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(proPlan)
                .amount(new BigDecimal("588.82"))
                .build();
    }

    @Test
    void shouldGenerateInvoiceWithCorrectTaxesAndSequentialNumber() {
        when(taxCalculationService.calculateTax(new BigDecimal("499.00"))).thenReturn(
                new TaxCalculationResult(new BigDecimal("499.00"), new BigDecimal("18.00"), new BigDecimal("89.82"), new BigDecimal("588.82"))
        );
        when(invoiceNumberGenerator.nextInvoiceNumber()).thenReturn("INV-202608-00001");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice invoice = invoiceService.generateInvoice(user, subscription, paymentOrder, proPlan, BillingInterval.MONTHLY);

        assertNotNull(invoice);
        assertEquals("INV-202608-00001", invoice.getInvoiceNumber());
        assertEquals(new BigDecimal("499.00"), invoice.getSubtotal());
        assertEquals(new BigDecimal("89.82"), invoice.getTaxAmount());
        assertEquals(new BigDecimal("588.82"), invoice.getTotalAmount());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(1, invoice.getItems().size());
        assertEquals("Pro Plan Subscription (1 Month)", invoice.getItems().get(0).getDescription());
    }

    @Test
    void shouldGetInvoicesForUser() {
        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("INV-202608-00001")
                .user(user)
                .subscription(subscription)
                .subtotal(new BigDecimal("499.00"))
                .taxAmount(new BigDecimal("89.82"))
                .totalAmount(new BigDecimal("588.82"))
                .status(InvoiceStatus.PAID)
                .createdAt(LocalDateTime.now())
                .build();

        when(invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(invoice));

        List<InvoiceDto> result = invoiceService.getInvoices(userId);

        assertEquals(1, result.size());
        assertEquals("INV-202608-00001", result.get(0).getInvoiceNumber());
        assertEquals(new BigDecimal("588.82"), result.get(0).getTotalAmount());
    }
}

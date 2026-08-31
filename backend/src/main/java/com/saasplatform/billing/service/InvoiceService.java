package com.saasplatform.billing.service;

import com.saasplatform.audit.entity.AuditAction;
import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.billing.dto.InvoiceDto;
import com.saasplatform.billing.dto.InvoiceItemDto;
import com.saasplatform.billing.dto.TaxCalculationResult;
import com.saasplatform.billing.entity.Invoice;
import com.saasplatform.billing.entity.InvoiceItem;
import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.billing.repository.InvoiceRepository;
import com.saasplatform.common.enums.BillingInterval;
import com.saasplatform.common.enums.InvoiceStatus;
import com.saasplatform.exception.ResourceNotFoundException;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final TaxCalculationService taxCalculationService;
    private final AuditLogService auditLogService;

    @Transactional
    public Invoice generateInvoice(
            User user,
            Subscription subscription,
            PaymentOrder paymentOrder,
            Plan plan,
            BillingInterval interval
    ) {
        BigDecimal subtotal = (interval == BillingInterval.YEARLY)
                ? plan.getPriceYearly()
                : plan.getPriceMonthly();

        TaxCalculationResult tax = taxCalculationService.calculateTax(subtotal);
        String invoiceNumber = invoiceNumberGenerator.nextInvoiceNumber();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .user(user)
                .subscription(subscription)
                .paymentOrder(paymentOrder)
                .subtotal(tax.getSubtotal())
                .taxAmount(tax.getTaxAmount())
                .totalAmount(tax.getTotalAmount())
                .currency(plan.getCurrency() != null ? plan.getCurrency() : "INR")
                .status(InvoiceStatus.PAID)
                .billingPeriodStart(subscription.getCurrentPeriodStart())
                .billingPeriodEnd(subscription.getCurrentPeriodEnd())
                .items(new ArrayList<>())
                .build();

        String itemDescription = String.format("%s Subscription (%s)",
                plan.getName(),
                interval == BillingInterval.YEARLY ? "1 Year" : "1 Month"
        );

        InvoiceItem item = InvoiceItem.builder()
                .invoice(invoice)
                .description(itemDescription)
                .quantity(1)
                .unitPrice(tax.getSubtotal())
                .amount(tax.getSubtotal())
                .build();

        invoice.getItems().add(item);
        invoice = invoiceRepository.save(invoice);

        auditLogService.logEvent(
                user.getId(),
                user.getEmail(),
                AuditAction.INVOICE_CREATED,
                "Invoice",
                invoice.getId() != null ? invoice.getId().toString() : invoiceNumber,
                "Generated invoice #" + invoiceNumber + " total=" + tax.getTotalAmount(),
                null
        );

        log.info("Generated invoice {} for userId={}, total={}", invoiceNumber, user.getId(), tax.getTotalAmount());
        return invoice;
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> getInvoices(UUID userId) {
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(UUID invoiceId, UUID userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserIdWithItems(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for this account"));
        return mapToDto(invoice);
    }

    public InvoiceDto mapToDto(Invoice invoice) {
        List<InvoiceItemDto> itemDtos = (invoice.getItems() != null)
                ? invoice.getItems().stream()
                .map(i -> InvoiceItemDto.builder()
                        .id(i.getId())
                        .description(i.getDescription())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .amount(i.getAmount())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        String planName = invoice.getSubscription() != null && invoice.getSubscription().getPlan() != null
                ? invoice.getSubscription().getPlan().getName() : "SaaS Plan";

        String customerName = invoice.getUser() != null
                ? invoice.getUser().getFirstName() + " " + invoice.getUser().getLastName() : "Customer";

        String customerEmail = invoice.getUser() != null ? invoice.getUser().getEmail() : null;

        return InvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .userId(invoice.getUser().getId())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .planName(planName)
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .billingPeriodStart(invoice.getBillingPeriodStart())
                .billingPeriodEnd(invoice.getBillingPeriodEnd())
                .createdAt(invoice.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}

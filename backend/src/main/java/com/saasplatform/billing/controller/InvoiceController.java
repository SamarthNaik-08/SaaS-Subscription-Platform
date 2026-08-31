package com.saasplatform.billing.controller;

import com.saasplatform.billing.dto.InvoiceDto;
import com.saasplatform.billing.service.InvoiceService;
import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoices(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<InvoiceDto> invoices = invoiceService.getInvoices(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(invoices, "Invoices retrieved successfully"));
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(
            @PathVariable UUID invoiceId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        InvoiceDto invoice = invoiceService.getInvoiceById(invoiceId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice retrieved successfully"));
    }
}

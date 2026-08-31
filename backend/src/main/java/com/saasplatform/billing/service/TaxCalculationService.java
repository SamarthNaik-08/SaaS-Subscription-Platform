package com.saasplatform.billing.service;

import com.saasplatform.billing.dto.TaxCalculationResult;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxCalculationService {

    @Getter
    @Value("${app.billing.tax-rate-percentage:18.0}")
    private double taxRatePercentage;

    public TaxCalculationResult calculateTax(BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return TaxCalculationResult.builder()
                    .subtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .taxRate(BigDecimal.valueOf(taxRatePercentage).setScale(2, RoundingMode.HALF_UP))
                    .taxAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .build();
        }

        BigDecimal scaledSubtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxRate = BigDecimal.valueOf(taxRatePercentage).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = scaledSubtotal.multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = scaledSubtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        return TaxCalculationResult.builder()
                .subtotal(scaledSubtotal)
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .build();
    }
}

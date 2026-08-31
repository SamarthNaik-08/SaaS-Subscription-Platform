package com.saasplatform.billing.service;

import com.saasplatform.billing.entity.InvoiceSequence;
import com.saasplatform.billing.repository.InvoiceSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceSequenceRepository sequenceRepository;
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Transactional(propagation = Propagation.REQUIRED)
    public String nextInvoiceNumber() {
        String periodKey = LocalDateTime.now().format(PERIOD_FORMATTER);

        InvoiceSequence sequence = sequenceRepository.findByPeriodKeyForUpdate(periodKey)
                .orElseGet(() -> {
                    InvoiceSequence newSeq = InvoiceSequence.builder()
                            .periodKey(periodKey)
                            .lastSequence(0L)
                            .build();
                    return sequenceRepository.saveAndFlush(newSeq);
                });

        long nextVal = sequence.getLastSequence() + 1;
        sequence.setLastSequence(nextVal);
        sequenceRepository.save(sequence);

        String formattedNumber = String.format("INV-%s-%05d", periodKey, nextVal);
        log.debug("Generated invoice number: {}", formattedNumber);
        return formattedNumber;
    }
}

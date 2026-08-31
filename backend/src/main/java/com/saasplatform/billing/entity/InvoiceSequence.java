package com.saasplatform.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "invoice_sequences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSequence {

    @Id
    @Column(name = "period_key", length = 10)
    private String periodKey; // e.g. "202608"

    @Column(name = "last_sequence", nullable = false)
    private Long lastSequence;
}

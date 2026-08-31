package com.saasplatform.billing.repository;

import com.saasplatform.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.items WHERE i.id = :id AND i.user.id = :userId")
    Optional<Invoice> findByIdAndUserIdWithItems(@Param("id") UUID id, @Param("userId") UUID userId);

    List<Invoice> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByInvoiceNumber(String invoiceNumber);
}

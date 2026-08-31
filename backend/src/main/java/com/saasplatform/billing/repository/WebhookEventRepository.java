package com.saasplatform.billing.repository;

import com.saasplatform.billing.entity.WebhookEvent;
import com.saasplatform.common.enums.PaymentGatewayProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Optional<WebhookEvent> findByProviderAndProviderEventId(PaymentGatewayProvider provider, String providerEventId);

    boolean existsByProviderAndProviderEventId(PaymentGatewayProvider provider, String providerEventId);
}

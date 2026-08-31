package com.saasplatform.billing.repository;

import com.saasplatform.billing.entity.PaymentOrder;
import com.saasplatform.common.enums.PaymentOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {

    Optional<PaymentOrder> findByGatewayOrderId(String gatewayOrderId);

    Optional<PaymentOrder> findByIdAndUserId(UUID id, UUID userId);

    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<PaymentOrder> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, PaymentOrderStatus status);
}

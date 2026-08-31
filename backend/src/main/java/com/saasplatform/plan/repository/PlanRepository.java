package com.saasplatform.plan.repository;

import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Optional<Plan> findByCode(PlanCode code);
    boolean existsByCode(PlanCode code);
}

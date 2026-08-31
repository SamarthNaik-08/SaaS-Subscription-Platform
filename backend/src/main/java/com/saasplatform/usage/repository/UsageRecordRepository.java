package com.saasplatform.usage.repository;

import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.usage.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {

    @Query("SELECT COALESCE(SUM(u.quantity), 0L) FROM UsageRecord u " +
           "WHERE u.user.id = :userId " +
           "AND u.metric = :metric " +
           "AND u.createdAt >= :periodStart " +
           "AND u.createdAt < :periodEnd")
    long sumQuantityByUserAndMetricAndPeriod(
            @Param("userId") UUID userId,
            @Param("metric") UsageMetric metric,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    @Query("SELECT u FROM UsageRecord u " +
           "WHERE u.user.id = :userId " +
           "AND u.createdAt >= :periodStart " +
           "AND u.createdAt < :periodEnd " +
           "ORDER BY u.createdAt DESC")
    List<UsageRecord> findByUserIdAndPeriod(
            @Param("userId") UUID userId,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    @Query("SELECT u FROM UsageRecord u " +
           "WHERE u.user.id = :userId " +
           "AND u.metric = :metric " +
           "ORDER BY u.createdAt DESC")
    List<UsageRecord> findTop50ByUserIdAndMetricOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("metric") UsageMetric metric
    );
}

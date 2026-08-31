package com.saasplatform.usage.entity;

import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usage_records", indexes = {
        @Index(name = "idx_usage_records_user_metric_period", columnList = "user_id, metric, period_start, period_end"),
        @Index(name = "idx_usage_records_user_id", columnList = "user_id"),
        @Index(name = "idx_usage_records_metric", columnList = "metric"),
        @Index(name = "idx_usage_records_period_start", columnList = "period_start"),
        @Index(name = "idx_usage_records_period_end", columnList = "period_end")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UsageMetric metric;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false, name = "period_start")
    private LocalDateTime periodStart;

    @Column(nullable = false, name = "period_end")
    private LocalDateTime periodEnd;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
}

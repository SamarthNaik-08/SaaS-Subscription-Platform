package com.saasplatform.usage.dto;

import com.saasplatform.common.enums.UsageMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecordDto {
    private UUID id;
    private UsageMetric metric;
    private Long quantity;
    private LocalDateTime createdAt;
    private String metadata;
    private String userName;
    private String userEmail;
}

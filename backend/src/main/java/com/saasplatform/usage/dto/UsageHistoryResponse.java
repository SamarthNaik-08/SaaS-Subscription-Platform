package com.saasplatform.usage.dto;

import com.saasplatform.common.enums.UsageMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageHistoryResponse {
    private UsageMetric metric;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private long totalUsed;
    private List<UsageRecordDto> records;
}

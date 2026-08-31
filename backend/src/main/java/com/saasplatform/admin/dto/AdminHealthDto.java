package com.saasplatform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminHealthDto {
    private String status;
    private LocalDateTime timestamp;
    private long uptimeSeconds;
    private String javaVersion;
    private int availableProcessors;
    private long totalMemoryBytes;
    private long freeMemoryBytes;
    private long maxMemoryBytes;
    private int activeThreads;
    private boolean databaseConnected;
    private Map<String, Object> details;
}

package com.saasplatform.notification.dto;

import com.saasplatform.notification.entity.Notification;
import com.saasplatform.notification.entity.NotificationType;
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
public class NotificationDto {
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private boolean isRead;
    private LocalDateTime readAt;
    private String metadata;
    private LocalDateTime createdAt;

    public static NotificationDto fromEntity(Notification n) {
        if (n == null) return null;
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.isRead())
                .readAt(n.getReadAt())
                .metadata(n.getMetadata())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

package com.saasplatform.notification.service;

import com.saasplatform.exception.ResourceNotFoundException;
import com.saasplatform.notification.dto.NotificationDto;
import com.saasplatform.notification.entity.Notification;
import com.saasplatform.notification.entity.NotificationType;
import com.saasplatform.notification.repository.NotificationRepository;
import com.saasplatform.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public Notification createNotification(User user, NotificationType type, String title, String message, String metadata) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .metadata(metadata)
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("Created notification [{}] for user: {}", type, user.getEmail());

        // Dispatch optional email notification safely
        try {
            emailNotificationService.sendEmail(user.getEmail(), title, message);
        } catch (Exception e) {
            log.warn("Failed to send email notification to {}: {}", user.getEmail(), e.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void checkAndSendQuotaThresholdAlert(User user, long used, long limit, LocalDateTime periodStart) {
        if (limit <= 0) return;

        double usageRatio = (double) used / limit;

        if (used >= limit) {
            // 100% Quota Exceeded
            if (!notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(user.getId(), NotificationType.QUOTA_EXCEEDED, periodStart)) {
                createNotification(
                        user,
                        NotificationType.QUOTA_EXCEEDED,
                        "Monthly AI Quota Exceeded (100%)",
                        String.format("You have reached 100%% of your monthly quota (%d/%d requests). Upgrade your plan to continue using AI Studio.", used, limit),
                        "{\"used\":" + used + ",\"limit\":" + limit + "}"
                );
            }
        } else if (usageRatio >= 0.90) {
            // 90% Warning
            if (!notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(user.getId(), NotificationType.USAGE_WARNING_90, periodStart)) {
                createNotification(
                        user,
                        NotificationType.USAGE_WARNING_90,
                        "AI Quota Warning (90% Used)",
                        String.format("You have consumed 90%% of your monthly AI quota (%d/%d requests). Consider upgrading your plan soon.", used, limit),
                        "{\"used\":" + used + ",\"limit\":" + limit + "}"
                );
            }
        } else if (usageRatio >= 0.75) {
            // 75% Warning
            if (!notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(user.getId(), NotificationType.USAGE_WARNING_75, periodStart)) {
                createNotification(
                        user,
                        NotificationType.USAGE_WARNING_75,
                        "AI Quota Notice (75% Used)",
                        String.format("You have consumed 75%% of your monthly AI quota (%d/%d requests).", used, limit),
                        "{\"used\":" + used + ",\"limit\":" + limit + "}"
                );
            }
        }
    }
}

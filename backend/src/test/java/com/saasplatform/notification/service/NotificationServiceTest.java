package com.saasplatform.notification.service;

import com.saasplatform.common.enums.GlobalRole;
import com.saasplatform.notification.dto.NotificationDto;
import com.saasplatform.notification.entity.NotificationType;
import com.saasplatform.notification.repository.NotificationRepository;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("notify-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .firstName("Notify")
                .lastName("User")
                .globalRole(GlobalRole.USER)
                .build());
    }

    @Test
    void shouldCreateAndMarkNotificationsAsRead() {
        notificationService.createNotification(user, NotificationType.SECURITY_ALERT, "Alert 1", "Message 1", null);
        notificationService.createNotification(user, NotificationType.SECURITY_ALERT, "Alert 2", "Message 2", null);

        long unreadCount = notificationService.getUnreadCount(user.getId());
        assertEquals(2, unreadCount);

        List<NotificationDto> notifications = notificationService.getUserNotifications(user.getId());
        assertEquals(2, notifications.size());

        // Mark first as read
        notificationService.markAsRead(user.getId(), notifications.get(0).getId());
        assertEquals(1, notificationService.getUnreadCount(user.getId()));

        // Mark all as read
        notificationService.markAllAsRead(user.getId());
        assertEquals(0, notificationService.getUnreadCount(user.getId()));
    }

    @Test
    void shouldSendQuotaThresholdWarningsIdempotently() {
        LocalDateTime periodStart = LocalDateTime.now().minusDays(5);

        // First warning at 75% -> Should create notification
        notificationService.checkAndSendQuotaThresholdAlert(user, 75L, 100L, periodStart);
        assertEquals(1, notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());

        // Second call with same 75% in same period -> Should be idempotent (no duplicate notification)
        notificationService.checkAndSendQuotaThresholdAlert(user, 76L, 100L, periodStart);
        assertEquals(1, notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());

        // Call with 90% in same period -> Should create second distinct notification
        notificationService.checkAndSendQuotaThresholdAlert(user, 90L, 100L, periodStart);
        assertEquals(2, notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());

        // Call with 100% in same period -> Should create third distinct notification
        notificationService.checkAndSendQuotaThresholdAlert(user, 100L, 100L, periodStart);
        assertEquals(3, notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());
    }
}

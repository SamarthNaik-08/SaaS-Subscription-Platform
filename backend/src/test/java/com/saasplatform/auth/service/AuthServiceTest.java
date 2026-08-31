package com.saasplatform.auth.service;

import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.LoginRequest;
import com.saasplatform.auth.dto.RefreshTokenRequest;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.common.enums.SubscriptionStatus;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.UnauthorizedException;
import com.saasplatform.notification.entity.Notification;
import com.saasplatform.notification.repository.NotificationRepository;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.repository.SubscriptionRepository;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldRegisterNewUserWithFreeSubscriptionAndWelcomeNotification() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Samarth")
                .lastName("Naik")
                .email("samarth.test@example.com")
                .password("StrongPassword123")
                .build();

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("samarth.test@example.com", response.getUser().getEmail());

        // Verify User in DB
        User user = userRepository.findByEmail("samarth.test@example.com").orElse(null);
        assertNotNull(user);
        assertEquals("Samarth", user.getFirstName());

        // Verify Subscription directly attached to User
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(user.getId());
        assertEquals(1, subscriptions.size());
        Subscription subscription = subscriptions.get(0);
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(PlanCode.FREE, subscription.getPlan().getCode());
        assertEquals(50, subscription.getPlan().getMonthlyAiLimit());

        // Verify Welcome notification created
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertFalse(notifications.isEmpty());
        assertEquals("Welcome to AI SaaS Platform!", notifications.get(0).getTitle());
    }

    @Test
    void shouldFailOnDuplicateEmailRegistration() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("duplicate@example.com")
                .password("StrongPassword123")
                .build();

        authService.register(request);

        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("duplicate@example.com")
                .password("AnotherPassword123")
                .build();

        assertThrows(BadRequestException.class, () -> authService.register(duplicateRequest));
    }

    @Test
    void shouldAuthenticateValidLogin() {
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Alex")
                .lastName("Smith")
                .email("alex@example.com")
                .password("MySecretPass123")
                .build();
        authService.register(registerReq);

        LoginRequest loginReq = LoginRequest.builder()
                .email("alex@example.com")
                .password("MySecretPass123")
                .build();

        AuthResponse loginResponse = authService.login(loginReq);
        assertNotNull(loginResponse);
        assertNotNull(loginResponse.getAccessToken());
        assertNotNull(loginResponse.getRefreshToken());
        assertEquals("alex@example.com", loginResponse.getUser().getEmail());
    }

    @Test
    void shouldRotateRefreshTokenCorrectly() {
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Rotation")
                .lastName("User")
                .email("rotation@example.com")
                .password("StrongPassword123")
                .build();
        AuthResponse registerResponse = authService.register(registerReq);
        String oldRefreshToken = registerResponse.getRefreshToken();

        // Perform Refresh
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(oldRefreshToken);
        AuthResponse newTokens = authService.refreshToken(refreshReq);

        assertNotNull(newTokens.getAccessToken());
        assertNotNull(newTokens.getRefreshToken());
        assertNotEquals(oldRefreshToken, newTokens.getRefreshToken());

        // Old token should now be revoked and rejected
        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(new RefreshTokenRequest(oldRefreshToken)));
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() {
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Logout")
                .lastName("User")
                .email("logout@example.com")
                .password("StrongPassword123")
                .build();
        AuthResponse registerResponse = authService.register(registerReq);
        String refreshToken = registerResponse.getRefreshToken();

        // Logout
        authService.logout(new RefreshTokenRequest(refreshToken));

        // Refresh token should no longer work
        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(new RefreshTokenRequest(refreshToken)));
    }
}

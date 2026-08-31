package com.saasplatform.user.service;

import com.saasplatform.audit.entity.AuditAction;
import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.auth.service.AuthService;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.ResourceNotFoundException;
import com.saasplatform.notification.entity.NotificationType;
import com.saasplatform.notification.service.NotificationService;
import com.saasplatform.refresh.entity.RefreshToken;
import com.saasplatform.refresh.repository.RefreshTokenRepository;
import com.saasplatform.user.dto.ChangePasswordRequest;
import com.saasplatform.user.dto.SessionDto;
import com.saasplatform.user.dto.UpdateProfileRequest;
import com.saasplatform.user.dto.UserDto;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public UserDto getProfile(UUID userId) {
        return authService.getCurrentUser(userId);
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user = userRepository.save(user);

        log.info("Updated profile for user: {}", user.getEmail());
        return authService.mapToUserDto(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Password change failed: incorrect current password for user: {}", user.getEmail());
            throw new BadRequestException("Current password is incorrect");
        }

        // 2. Hash and save new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 3. Revoke all active refresh tokens for session security
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        tokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);

        // 4. Record audit log
        auditLogService.logEvent(
                user.getId(),
                user.getEmail(),
                AuditAction.PASSWORD_CHANGED,
                "User",
                user.getId().toString(),
                "User changed password; all active sessions revoked",
                null
        );

        // 5. Send security alert notification
        notificationService.createNotification(
                user,
                NotificationType.SECURITY_ALERT,
                "Password Changed Successfully",
                "Your account password was recently changed. All other active sessions have been signed out.",
                "{}"
        );

        log.info("Password changed and all refresh tokens revoked for user: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getActiveSessions(UUID userId) {
        return refreshTokenRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(SessionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeAllSessions(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        tokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);
        log.info("Revoked all active sessions for user: {}", user.getEmail());
    }
}

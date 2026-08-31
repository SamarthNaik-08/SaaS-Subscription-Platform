package com.saasplatform.auth.service;

import com.saasplatform.audit.entity.AuditAction;
import com.saasplatform.audit.service.AuditLogService;
import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.LoginRequest;
import com.saasplatform.auth.dto.RefreshTokenRequest;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.common.enums.GlobalRole;
import com.saasplatform.common.enums.PlanCode;
import com.saasplatform.common.enums.SubscriptionStatus;
import com.saasplatform.common.enums.UserStatus;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.ResourceNotFoundException;
import com.saasplatform.exception.UnauthorizedException;
import com.saasplatform.notification.entity.NotificationType;
import com.saasplatform.notification.service.NotificationService;
import com.saasplatform.plan.entity.Plan;
import com.saasplatform.plan.repository.PlanRepository;
import com.saasplatform.refresh.entity.RefreshToken;
import com.saasplatform.refresh.repository.RefreshTokenRepository;
import com.saasplatform.security.JwtService;
import com.saasplatform.security.UserPrincipal;
import com.saasplatform.subscription.entity.Subscription;
import com.saasplatform.subscription.repository.SubscriptionRepository;
import com.saasplatform.user.dto.UserDto;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing user registration for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            log.warn("Registration rejected: Email already registered {}", request.getEmail());
            throw new BadRequestException("Email is already registered");
        }

        // 1. Create and save User
        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .globalRole(GlobalRole.USER)
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);
        log.info("User created with ID: {}", user.getId());

        // 2. Find or create FREE Plan
        Plan freePlan = planRepository.findByCode(PlanCode.FREE)
                .orElseGet(() -> planRepository.save(Plan.builder()
                        .code(PlanCode.FREE)
                        .name("Free Plan")
                        .description("Essential starter plan for individuals")
                        .priceMonthly(BigDecimal.ZERO)
                        .priceYearly(BigDecimal.ZERO)
                        .currency("INR")
                        .monthlyAiLimit(50)
                        .storageLimitMb(100L)
                        .isActive(true)
                        .build()));

        // 3. Create FREE ACTIVE Subscription directly bound to user
        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .currentPeriodStart(now)
                .currentPeriodEnd(now.plusMonths(1))
                .cancelAtPeriodEnd(false)
                .build();
        subscriptionRepository.save(subscription);
        log.info("FREE subscription created for user: {}", user.getId());

        // 4. Send Welcome notification
        notificationService.createNotification(
                user,
                NotificationType.WELCOME,
                "Welcome to AI SaaS Platform!",
                "Your account has been created with a complimentary Free Plan (50 AI requests/month). Enjoy building!",
                "{}"
        );

        // 5. Log audit event
        auditLogService.logEvent(
                user.getId(),
                user.getEmail(),
                AuditAction.REGISTER_SUCCESS,
                "User",
                user.getId().toString(),
                "User registered and assigned FREE plan",
                null
        );

        // 6. Generate Tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getGlobalRole().name());
        String rawRefreshToken = jwtService.generateRefreshToken();
        saveRefreshToken(user, rawRefreshToken);

        UserDto userDto = mapToUserDto(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .user(userDto)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login for email: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().trim().toLowerCase(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getGlobalRole().name());
            String rawRefreshToken = jwtService.generateRefreshToken();
            saveRefreshToken(user, rawRefreshToken);

            auditLogService.logEvent(
                    user.getId(),
                    user.getEmail(),
                    AuditAction.LOGIN_SUCCESS,
                    "User",
                    user.getId().toString(),
                    "User logged in successfully",
                    null
            );

            log.info("Login successful for user: {}", user.getEmail());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(rawRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                    .user(mapToUserDto(user))
                    .build();
        } catch (Exception e) {
            auditLogService.logEvent(
                    null,
                    request.getEmail(),
                    AuditAction.LOGIN_FAILED,
                    "User",
                    null,
                    "Login attempt failed: " + e.getMessage(),
                    null
            );
            throw e;
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Refresh token cannot be blank");
        }

        String tokenHash = jwtService.hashToken(rawToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Attempted use of revoked or expired refresh token");
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        // Revoke old token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is inactive or suspended");
        }

        // Issue new rotated refresh token & new access token
        String newRawRefreshToken = jwtService.generateRefreshToken();
        saveRefreshToken(user, newRawRefreshToken);

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getGlobalRole().name());

        auditLogService.logEvent(
                user.getId(),
                user.getEmail(),
                AuditAction.TOKEN_REFRESH,
                "RefreshToken",
                storedToken.getId().toString(),
                "Refresh token rotated successfully",
                null
        );

        log.info("Rotated refresh token successfully for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .user(mapToUserDto(user))
                .build();
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            String tokenHash = jwtService.hashToken(request.getRefreshToken());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                auditLogService.logEvent(
                        token.getUser().getId(),
                        token.getUser().getEmail(),
                        AuditAction.LOGOUT,
                        "RefreshToken",
                        token.getId().toString(),
                        "User logged out and session revoked",
                        null
                );
                log.info("Revoked refresh token on logout for user: {}", token.getUser().getEmail());
            });
        }
        SecurityContextHolder.clearContext();
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToUserDto(user);
    }

    private void saveRefreshToken(User user, String rawRefreshToken) {
        String tokenHash = jwtService.hashToken(rawRefreshToken);
        Instant expiresAt = Instant.now().plusMillis(jwtService.getRefreshExpirationMs());

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
    }

    public UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getGlobalRole())
                .emailVerified(user.isEmailVerified())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}

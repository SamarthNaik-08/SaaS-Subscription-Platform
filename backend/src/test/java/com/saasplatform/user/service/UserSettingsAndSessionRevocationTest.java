package com.saasplatform.user.service;

import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.RefreshTokenRequest;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.auth.service.AuthService;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.UnauthorizedException;
import com.saasplatform.user.dto.ChangePasswordRequest;
import com.saasplatform.user.dto.SessionDto;
import com.saasplatform.user.dto.UpdateProfileRequest;
import com.saasplatform.user.dto.UserDto;
import com.saasplatform.user.entity.User;
import com.saasplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserSettingsAndSessionRevocationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldUpdateProfileDetails() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Original")
                .lastName("Name")
                .email("profile-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        UserDto updated = userService.updateProfile(userId, new UpdateProfileRequest("UpdatedFirst", "UpdatedLast"));
        assertEquals("UpdatedFirst", updated.getFirstName());
        assertEquals("UpdatedLast", updated.getLastName());

        User inDb = userRepository.findById(userId).orElseThrow();
        assertEquals("UpdatedFirst", inDb.getFirstName());
        assertEquals("UpdatedLast", inDb.getLastName());
    }

    @Test
    void shouldChangePasswordAndRevokeAllActiveSessions() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Sec")
                .lastName("User")
                .email("sec-" + UUID.randomUUID() + "@test.com")
                .password("OldPassword123")
                .build());

        UUID userId = authRes.getUser().getId();
        String oldRefreshToken = authRes.getRefreshToken();

        // 1. Trying to change password with wrong current password throws BadRequestException
        assertThrows(BadRequestException.class, () ->
                userService.changePassword(userId, new ChangePasswordRequest("WrongPass", "NewPassword123")));

        // 2. Change password with correct current password succeeds
        userService.changePassword(userId, new ChangePasswordRequest("OldPassword123", "NewPassword123"));

        // 3. Old refresh token must be revoked and rejected
        assertThrows(UnauthorizedException.class, () ->
                authService.refreshToken(new RefreshTokenRequest(oldRefreshToken)));

        // 4. Check sessions list
        List<SessionDto> sessions = userService.getActiveSessions(userId);
        assertFalse(sessions.isEmpty());
        assertTrue(sessions.get(0).isRevoked());
    }
}

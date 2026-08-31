package com.saasplatform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", secretKey);
        ReflectionTestUtils.setField(jwtService, "jwtAccessExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpirationMs", 604800000L);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String role = "USER";

        String token = jwtService.generateAccessToken(userId, email, role);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(email, jwtService.extractEmail(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.jwt.token"));
    }

    @Test
    void shouldHashRefreshTokenConsistently() {
        String rawToken = "raw-refresh-token-12345";
        String hash1 = jwtService.hashToken(rawToken);
        String hash2 = jwtService.hashToken(rawToken);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertNotEquals(rawToken, hash1);
    }
}

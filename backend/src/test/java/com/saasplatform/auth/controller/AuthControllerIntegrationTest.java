package com.saasplatform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.auth.dto.RefreshTokenRequest;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Test
    void shouldRegisterAndReturn201WithTokens() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Integration")
                .lastName("Test")
                .email("integration@test.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("integration@test.com"));
    }

    @Test
    void shouldRejectDuplicateEmailWith400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("First")
                .lastName("User")
                .email("duplicate.api@test.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldLoginAndAccessProtectedEndpoints() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Protected")
                .lastName("Tester")
                .email("protected@test.com")
                .password("SecurePass123")
                .build();

        var authRes = authService.register(registerReq);
        String accessToken = authRes.getAccessToken();

        // 1. Unauthenticated request to /api/v1/users/me should fail with 401
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        // 2. Authenticated request with Bearer JWT should succeed with 200
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("protected@test.com"));

        // 3. User subscription endpoint should return FREE Plan & ACTIVE Subscription
        mockMvc.perform(get("/api/v1/billing/subscription/current")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.plan.code").value("FREE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldRotateTokenAndLogoutCleanly() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Refresh")
                .lastName("Tester")
                .email("refresh.flow@test.com")
                .password("SecurePass123")
                .build();

        var authRes = authService.register(registerReq);
        String refreshToken = authRes.getRefreshToken();

        // Refresh token endpoint
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(refreshToken);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk());
    }
}

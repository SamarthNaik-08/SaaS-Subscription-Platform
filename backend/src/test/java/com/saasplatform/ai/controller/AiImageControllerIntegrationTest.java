package com.saasplatform.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.ai.dto.AiImageRequest;
import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.auth.service.AuthService;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.usage.service.UsageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiImageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsageService usageService;

    @Test
    void shouldRejectUnauthenticatedImageGenerationWith401() throws Exception {
        AiImageRequest request = AiImageRequest.builder()
                .prompt("Cyberpunk city neon lights")
                .aspectRatio("16:9")
                .stylePreset("Cinematic")
                .build();

        mockMvc.perform(post("/api/v1/ai/image/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGenerateImageForAuthenticatedUser() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Artist")
                .lastName("User")
                .email("artist-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        AiImageRequest request = AiImageRequest.builder()
                .prompt("A majestic mountain landscape at sunrise")
                .aspectRatio("16:9")
                .stylePreset("Photorealistic")
                .build();

        mockMvc.perform(post("/api/v1/ai/image/generate")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.prompt").value("A majestic mountain landscape at sunrise"))
                .andExpect(jsonPath("$.data.aspectRatio").value("16:9"))
                .andExpect(jsonPath("$.data.quotaUsage.used").value(1))
                .andExpect(jsonPath("$.data.quotaUsage.remaining").value(49));
    }

    @Test
    void shouldRejectBlankPromptWith400() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Blank")
                .lastName("Prompt")
                .email("blank-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        AiImageRequest request = AiImageRequest.builder()
                .prompt("   ")
                .build();

        mockMvc.perform(post("/api/v1/ai/image/generate")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectImageGenerationWhenQuotaExhaustedWith429() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Exhausted")
                .lastName("Artist")
                .email("exhausted-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();
        // Exhaust the 50 free AI credits
        usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 50L, "Exhaust free tier limit");

        AiImageRequest request = AiImageRequest.builder()
                .prompt("Another image when out of credits")
                .build();

        mockMvc.perform(post("/api/v1/ai/image/generate")
                        .header("Authorization", "Bearer " + authRes.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("QUOTA_EXCEEDED"));
    }

    @Test
    void shouldListImageModelsSuccessfully() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Model")
                .lastName("Viewer")
                .email("models-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        mockMvc.perform(get("/api/v1/ai/image/models")
                        .header("Authorization", "Bearer " + authRes.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}

package com.saasplatform.ai.service;

import com.saasplatform.ai.dto.AiGenerateRequest;
import com.saasplatform.ai.dto.AiGenerateResponse;
import com.saasplatform.ai.dto.AiImageRequest;
import com.saasplatform.ai.dto.AiImageResponse;
import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.auth.service.AuthService;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.exception.QuotaExceededException;
import com.saasplatform.usage.service.UsageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiServiceTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsageService usageService;

    @Test
    void shouldGenerateAiResponseAndRecordQuota() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("AI")
                .lastName("User")
                .email("ai-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        AiGenerateRequest req = AiGenerateRequest.builder()
                .prompt("Summarize the benefits of consumer AI applications")
                .model("gemini-1.5-flash")
                .build();

        AiGenerateResponse response = aiService.generateText(userId, req);

        assertNotNull(response);
        assertNotNull(response.getText());
        assertEquals("gemini-1.5-flash", response.getModel());
        assertEquals(1L, response.getQuotaUsage().getUsed());
        assertEquals(49L, response.getQuotaUsage().getRemaining());
    }

    @Test
    void shouldRejectAiGenerationWhenQuotaExhausted() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("AI")
                .lastName("Exhausted")
                .email("ai-exhausted-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        // Exhaust the 50 free credits
        usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 50L, "Exhaust free quota");

        AiGenerateRequest req = AiGenerateRequest.builder()
                .prompt("Another prompt when limit reached")
                .model("gemini-1.5-flash")
                .build();

        assertThrows(QuotaExceededException.class, () -> aiService.generateText(userId, req));
    }

    @Test
    void shouldGenerateAiImageAndRecordQuota() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Image")
                .lastName("User")
                .email("image-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        AiImageRequest req = AiImageRequest.builder()
                .prompt("Futuristic cyberpunk city at night with neon lights")
                .aspectRatio("16:9")
                .stylePreset("Cinematic")
                .build();

        AiImageResponse response = aiService.generateImage(userId, req);

        assertNotNull(response);
        assertNotNull(response.getImageUrl());
        assertEquals("16:9", response.getAspectRatio());
        assertEquals("Cinematic", response.getStylePreset());
        assertEquals(1L, response.getQuotaUsage().getUsed());
        assertEquals(49L, response.getQuotaUsage().getRemaining());
    }

    @Test
    void shouldRejectImageGenerationWhenQuotaExhausted() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Image")
                .lastName("Exhausted")
                .email("image-exhausted-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        // Exhaust 50 quota units
        usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 50L, "Exhaust free quota");

        AiImageRequest req = AiImageRequest.builder()
                .prompt("Generate image with zero remaining credits")
                .build();

        assertThrows(QuotaExceededException.class, () -> aiService.generateImage(userId, req));
    }
}

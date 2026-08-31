package com.saasplatform.ai.service;

import com.saasplatform.ai.dto.AiGenerateResponse;
import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.auth.service.AuthService;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.exception.BadRequestException;
import com.saasplatform.exception.QuotaExceededException;
import com.saasplatform.usage.service.UsageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiMultimodalServiceTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsageService usageService;

    @Test
    void shouldProcessMultimodalImageAndRecordQuota() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Vision")
                .lastName("ServiceTest")
                .email("vision-service-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        MockMultipartFile image = new MockMultipartFile(
                "files", "screenshot.png", "image/png", "fake-png-bytes".getBytes()
        );

        AiGenerateResponse response = aiService.processMultimodal(
                userId, "What is shown in this image?", List.of(image), "gemini-2.0-flash", Map.of()
        );

        assertNotNull(response);
        assertNotNull(response.getText());
        assertTrue(response.getText().contains("Multimodal Analysis"));
        assertEquals(1L, response.getQuotaUsage().getUsed());
        assertEquals(49L, response.getQuotaUsage().getRemaining());
    }

    @Test
    void shouldRejectMultimodalWhenQuotaExhausted() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Exhausted")
                .lastName("ServiceTest")
                .email("exhausted-service-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        // Exhaust 50 free credits
        usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 50L, "Exhaust free quota");

        MockMultipartFile doc = new MockMultipartFile(
                "files", "doc.txt", "text/plain", "Sample content".getBytes()
        );

        assertThrows(QuotaExceededException.class, () ->
                aiService.processMultimodal(userId, "Analyze this", List.of(doc), "gemini-2.0-flash", Map.of())
        );
    }

    @Test
    void shouldRejectDisallowedFileExtension() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Security")
                .lastName("ServiceTest")
                .email("security-service-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();

        MockMultipartFile script = new MockMultipartFile(
                "files", "deploy.sh", "text/x-shellscript", "echo hack".getBytes()
        );

        assertThrows(BadRequestException.class, () ->
                aiService.processMultimodal(userId, "Run script", List.of(script), "gemini-2.0-flash", Map.of())
        );
    }
}

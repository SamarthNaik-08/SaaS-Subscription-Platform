package com.saasplatform.ai.controller;

import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.auth.service.AuthService;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.usage.service.UsageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiMultimodalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UsageService usageService;

    @Test
    void shouldRejectUnauthenticatedMultimodalRequestWith401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.png", "image/png", new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/ai/multimodal")
                        .file(file)
                        .param("prompt", "Analyze this image"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldProcessImageMultimodalForAuthenticatedUser() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Vision")
                .lastName("User")
                .email("vision-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        MockMultipartFile imageFile = new MockMultipartFile(
                "files", "diagram.png", "image/png", "fake-png-binary-content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ai/multimodal")
                        .file(imageFile)
                        .param("prompt", "Describe this diagram")
                        .header("Authorization", "Bearer " + authRes.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").isNotEmpty())
                .andExpect(jsonPath("$.data.quotaUsage.used").value(1))
                .andExpect(jsonPath("$.data.quotaUsage.remaining").value(49));
    }

    @Test
    void shouldProcessPdfMultimodalForAuthenticatedUser() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Pdf")
                .lastName("User")
                .email("pdf-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        MockMultipartFile pdfFile = new MockMultipartFile(
                "files", "whitepaper.pdf", "application/pdf", "%PDF-1.4 Mock PDF Content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ai/multimodal")
                        .file(pdfFile)
                        .param("prompt", "Summarize this whitepaper")
                        .header("Authorization", "Bearer " + authRes.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").isNotEmpty());
    }

    @Test
    void shouldProcessSourceCodeMultimodalForAuthenticatedUser() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Code")
                .lastName("Reviewer")
                .email("code-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        String javaCode = """
                public class Calculator {
                    public int divide(int a, int b) {
                        return a / b;
                    }
                }
                """;

        MockMultipartFile codeFile = new MockMultipartFile(
                "files", "Calculator.java", "text/x-java-source", javaCode.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ai/multimodal")
                        .file(codeFile)
                        .param("prompt", "Find potential bugs in this code")
                        .header("Authorization", "Bearer " + authRes.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").isNotEmpty());
    }

    @Test
    void shouldRejectExecutableFileWith400() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Hacker")
                .lastName("Attempt")
                .email("hacker-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        MockMultipartFile exeFile = new MockMultipartFile(
                "files", "malicious.exe", "application/octet-stream", "MZbinarypayload".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ai/multimodal")
                        .file(exeFile)
                        .param("prompt", "Run this binary")
                        .header("Authorization", "Bearer " + authRes.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldRejectEmptyFileWith400() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Empty")
                .lastName("File")
                .email("empty-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        MockMultipartFile emptyFile = new MockMultipartFile(
                "files", "empty.txt", "text/plain", new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/ai/multimodal")
                        .file(emptyFile)
                        .param("prompt", "Analyze empty file")
                        .header("Authorization", "Bearer " + authRes.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldRejectOversizedFileWith413() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Large")
                .lastName("Upload")
                .email("large-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        // 11 MB file exceeds 10MB limit
        byte[] largeBytes = new byte[11 * 1024 * 1024];

        MockMultipartFile largeFile = new MockMultipartFile(
                "files", "large_dataset.csv", "text/csv", largeBytes
        );

        mockMvc.perform(multipart("/api/v1/ai/multimodal")
                        .file(largeFile)
                        .param("prompt", "Parse giant CSV")
                        .header("Authorization", "Bearer " + authRes.getAccessToken()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PAYLOAD_TOO_LARGE"));
    }

    @Test
    void shouldRejectMultimodalWhenQuotaExhaustedWith429() throws Exception {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("Exhausted")
                .lastName("Multimodal")
                .email("exhausted-multi-" + UUID.randomUUID() + "@test.com")
                .password("Password123")
                .build());

        UUID userId = authRes.getUser().getId();
        // Exhaust 50 free credits
        usageService.recordUsage(userId, UsageMetric.AI_REQUEST, 50L, "Exhaust free quota");

        MockMultipartFile validFile = new MockMultipartFile(
                "files", "doc.txt", "text/plain", "Hello world".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/ai/multimodal")
                        .file(validFile)
                        .param("prompt", "Analyze when limit reached")
                        .header("Authorization", "Bearer " + authRes.getAccessToken()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("QUOTA_EXCEEDED"));
    }
}

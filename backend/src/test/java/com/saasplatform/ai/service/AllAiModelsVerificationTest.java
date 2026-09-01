package com.saasplatform.ai.service;

import com.saasplatform.ai.dto.AiGenerateRequest;
import com.saasplatform.ai.dto.AiGenerateResponse;
import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
public class AllAiModelsVerificationTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private AuthService authService;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        AuthResponse authRes = authService.register(RegisterRequest.builder()
                .firstName("ModelVerifier")
                .lastName("QA")
                .email("model-qa-" + UUID.randomUUID() + "@test.com")
                .password("SecurePass123!")
                .build());
        testUserId = authRes.getUser().getId();
    }

    @Test
    @DisplayName("Verify Gemini 1.5 Flash Model — 'What is Java?'")
    void testGemini15FlashModel() {
        AiGenerateRequest request = AiGenerateRequest.builder()
                .model("gemini-1.5-flash")
                .prompt("What is Java?")
                .build();

        AiGenerateResponse response = aiService.generateText(testUserId, request);

        assertNotNull(response, "Response must not be null");
        assertEquals("gemini-1.5-flash", response.getModel());
        assertNotNull(response.getText(), "Response text must not be null");
        assertFalse(response.getText().contains("404 NOT_FOUND"), "Must not contain 404 error");
        assertFalse(response.getText().contains("is not found"), "Must not contain model not found error");

        String text = response.getText().toLowerCase();
        assertTrue(text.contains("java"), "Must explain Java");
        assertTrue(text.contains("jvm") || text.contains("bytecode") || text.contains("object-oriented") || text.contains("platform"), 
                "Must explain core Java principles (JVM, bytecode, platform independence)");
    }

    @Test
    @DisplayName("Verify Gemini 2.0 Flash Model — 'Explain Object-Oriented Programming (OOP)'")
    void testGemini20FlashModel() {
        AiGenerateRequest request = AiGenerateRequest.builder()
                .model("gemini-2.0-flash")
                .prompt("Explain Object-Oriented Programming (OOP)")
                .build();

        AiGenerateResponse response = aiService.generateText(testUserId, request);

        assertNotNull(response);
        assertEquals("gemini-2.0-flash", response.getModel());
        assertNotNull(response.getText());
        assertFalse(response.getText().contains("404 NOT_FOUND"));

        String text = response.getText().toLowerCase();
        assertTrue(text.contains("encapsulation") || text.contains("inheritance") || text.contains("polymorphism") || text.contains("abstraction"),
                "Must explain OOP core pillars");
    }

    @Test
    @DisplayName("Verify Gemini 1.5 Pro Model — 'What is React and how does it work?'")
    void testGemini15ProModel() {
        AiGenerateRequest request = AiGenerateRequest.builder()
                .model("gemini-1.5-pro")
                .prompt("What is React and how does it work?")
                .build();

        AiGenerateResponse response = aiService.generateText(testUserId, request);

        assertNotNull(response);
        assertEquals("gemini-1.5-pro", response.getModel());
        assertNotNull(response.getText());
        assertFalse(response.getText().contains("404 NOT_FOUND"));

        String text = response.getText().toLowerCase();
        assertTrue(text.contains("component") || text.contains("virtual dom") || text.contains("declarative") || text.contains("ui"),
                "Must explain React components, VDOM, or UI paradigms");
    }

    @Test
    @DisplayName("Verify Gemini 2.5 Flash Model — 'What is Python and its use cases?'")
    void testGemini25FlashModel() {
        AiGenerateRequest request = AiGenerateRequest.builder()
                .model("gemini-2.5-flash")
                .prompt("What is Python and its use cases?")
                .build();

        AiGenerateResponse response = aiService.generateText(testUserId, request);

        assertNotNull(response);
        assertEquals("gemini-2.5-flash", response.getModel());
        assertNotNull(response.getText());
        assertFalse(response.getText().contains("404 NOT_FOUND"));

        String text = response.getText().toLowerCase();
        assertTrue(text.contains("python"), "Must explain Python language");
        assertTrue(text.contains("machine learning") || text.contains("data") || text.contains("interpreted") || text.contains("web"),
                "Must detail Python capabilities");
    }

    @Test
    @DisplayName("Verify GPT-4o (Multimodal) Model — 'Explain Spring Boot architecture'")
    void testGpt4oModel() {
        AiGenerateRequest request = AiGenerateRequest.builder()
                .model("gpt-4o")
                .prompt("Explain Spring Boot architecture")
                .build();

        AiGenerateResponse response = aiService.generateText(testUserId, request);

        assertNotNull(response);
        assertEquals("gpt-4o", response.getModel());
        assertNotNull(response.getText());
        assertFalse(response.getText().contains("404 NOT_FOUND"));

        String text = response.getText().toLowerCase();
        assertTrue(text.contains("spring") || text.contains("boot") || text.contains("auto-configuration") || text.contains("framework"),
                "Must explain Spring Boot framework concepts");
    }

    @Test
    @DisplayName("Verify GPT-4o Mini Model — 'What is Java?'")
    void testGpt4oMiniModel() {
        AiGenerateRequest request = AiGenerateRequest.builder()
                .model("gpt-4o-mini")
                .prompt("What is Java?")
                .build();

        AiGenerateResponse response = aiService.generateText(testUserId, request);

        assertNotNull(response);
        assertEquals("gpt-4o-mini", response.getModel());
        assertNotNull(response.getText());
        assertFalse(response.getText().contains("404 NOT_FOUND"));

        String text = response.getText().toLowerCase();
        assertTrue(text.contains("java"), "Must explain Java");
        assertTrue(text.contains("jvm") || text.contains("bytecode") || text.contains("object-oriented") || text.contains("platform"),
                "Must detail Java fundamentals");
    }
}

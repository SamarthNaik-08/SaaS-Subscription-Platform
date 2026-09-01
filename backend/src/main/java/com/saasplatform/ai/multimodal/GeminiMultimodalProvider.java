package com.saasplatform.ai.multimodal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("geminiMultimodalProvider")
public class GeminiMultimodalProvider implements AiMultimodalProvider {

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    @Override
    public String getProviderName() {
        return "Google Gemini Vision & Multimodal";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("placeholder") && !apiKey.startsWith("your_");
    }

    @Override
    public String processMultimodal(String prompt, List<MultimodalAttachment> attachments, String model, Map<String, Object> options) {
        String targetModel = (model != null && !model.isBlank()) ? normalizeModel(model) : "gemini-2.5-flash";
        log.info("[Gemini Multimodal] Processing multimodal inference with model: {}, attachments: {}", 
                targetModel, attachments != null ? attachments.size() : 0);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");

            List<Map<String, Object>> parts = new ArrayList<>();

            // 1. Add Text Prompt Part
            String userPrompt = (prompt != null && !prompt.isBlank()) ? prompt : "Please analyze the attached files in detail.";
            parts.add(Map.of("text", userPrompt));

            // 2. Add Attachment Parts
            if (attachments != null) {
                for (MultimodalAttachment att : attachments) {
                    if (att.isImage() || att.isPdf()) {
                        Map<String, Object> inlineData = new HashMap<>();
                        inlineData.put("mimeType", att.getContentType());
                        inlineData.put("data", att.getBase64Data());
                        parts.add(Map.of("inlineData", inlineData));
                    } else if (att.isTextDocument()) {
                        String textSnippet = String.format("\n\n--- Attachment: %s (%s) ---\n%s\n--- End of %s ---\n",
                                att.getFileName(), att.getContentType(), att.getTextContent(), att.getFileName());
                        parts.add(Map.of("text", textSnippet));
                    }
                }
            }

            userContent.put("parts", parts);
            contents.add(userContent);
            requestBody.put("contents", contents);

            if (options != null && options.containsKey("systemInstruction") && options.get("systemInstruction") != null) {
                String sysInst = options.get("systemInstruction").toString().trim();
                if (!sysInst.isEmpty()) {
                    requestBody.put("systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", sysInst))
                    ));
                }
            }

            Map<String, Object> genConfig = new HashMap<>();
            if (options != null && options.containsKey("temperature")) {
                try {
                    genConfig.put("temperature", Double.parseDouble(options.get("temperature").toString()));
                } catch (Exception ignored) {}
            }
            genConfig.put("maxOutputTokens", 4096);
            requestBody.put("generationConfig", genConfig);

            return executeWithModelFallback(targetModel, requestBody);

        } catch (Exception e) {
            log.error("[Gemini Multimodal] Error processing multimodal request: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini Multimodal Error: " + e.getMessage(), e);
        }
    }

    private String executeWithModelFallback(String preferredModel, Map<String, Object> requestBody) {
        List<String[]> candidates = List.of(
                new String[]{"v1beta", preferredModel},
                new String[]{"v1beta", "gemini-2.5-flash"},
                new String[]{"v1beta", "gemini-2.0-flash"},
                new String[]{"v1beta", "gemini-2.5-pro"}
        );

        RestClientResponseException lastException = null;

        for (String[] candidate : candidates) {
            String apiVersion = candidate[0];
            String modelName = candidate[1];
            String url = String.format("%s/%s/models/%s:generateContent?key=%s", baseUrl, apiVersion, modelName, apiKey.trim());

            try {
                log.debug("Calling Gemini Multimodal API on {} / {}", apiVersion, modelName);
                String responseJson = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                return extractTextFromGeminiResponse(responseJson);

            } catch (RestClientResponseException e) {
                lastException = e;
                if (e.getStatusCode().value() == 404) {
                    log.warn("Gemini multimodal model {} on {} returned 404, trying fallback...", modelName, apiVersion);
                    continue;
                }
                return handleGeminiApiError(e);
            } catch (Exception e) {
                log.error("Network or unexpected error during Gemini Multimodal call: {}", e.getMessage(), e);
                return "⚠️ **Multimodal Error:** Unable to reach Google Gemini API (" + e.getMessage() + "). Please check your connection.";
            }
        }

        if (lastException != null) {
            return handleGeminiApiError(lastException);
        }

        return "⚠️ Unable to process multimodal content with available Google Gemini models.";
    }

    private String handleGeminiApiError(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        String detail = "";
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("error") && root.get("error").has("message")) {
                detail = root.get("error").get("message").asText();
            }
        } catch (Exception ignored) {}

        if (detail.isEmpty()) {
            detail = e.getMessage();
        }

        if (e.getStatusCode().value() == 404) {
            return "⚠️ The selected Google Gemini model is currently unavailable for multimodal inference. Please select another model.";
        }

        return String.format("⚠️ **Google Gemini API Error (%s):** %s", e.getStatusCode(), detail);
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return "gemini-2.5-flash";
        }
        if (model.contains("gemini-2.5-flash") || model.contains("gemini-3")) return "gemini-2.5-flash";
        if (model.contains("gemini-2.5-pro")) return "gemini-2.5-pro";
        if (model.contains("gemini-2.0-flash")) return "gemini-2.0-flash";
        if (model.contains("gemini-1.5-flash")) return "gemini-2.5-flash";
        if (model.contains("gemini-1.5-pro")) return "gemini-2.5-pro";
        return model.trim();
    }

    private String extractTextFromGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode parts = firstCandidate.path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    StringBuilder text = new StringBuilder();
                    for (JsonNode part : parts) {
                        if (part.has("text")) {
                            text.append(part.get("text").asText());
                        }
                    }
                    if (text.length() > 0) {
                        return text.toString();
                    }
                }
            }
            return "No content was generated by Gemini for this multimodal input.";
        } catch (Exception e) {
            log.error("Failed to parse Gemini multimodal response: {}", responseJson, e);
            return responseJson;
        }
    }
}

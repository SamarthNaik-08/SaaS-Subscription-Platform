package com.saasplatform.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.ai.dto.ChatMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("geminiAiProvider")
public class GeminiAiProvider implements AiProvider {

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;

    public GeminiAiProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String getProviderName() {
        return "Google Gemini";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("placeholder") && !apiKey.startsWith("your_");
    }

    @Override
    public String generateText(String prompt, String model, Map<String, Object> options) {
        String targetModel = normalizeModel(model);
        log.info("[Gemini Provider] Generating text with target model: {}", targetModel);

        Map<String, Object> requestBody = buildRequestBody(prompt, options);
        return executeWithModelFallback(targetModel, requestBody);
    }

    @Override
    public String chat(List<ChatMessageDto> messages, String model, Map<String, Object> options) {
        String targetModel = normalizeModel(model);
        log.info("[Gemini Provider] Chat turn with target model: {}", targetModel);

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();

        if (messages != null) {
            for (ChatMessageDto msg : messages) {
                String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "model";
                Map<String, Object> content = new HashMap<>();
                content.put("role", role);
                content.put("parts", List.of(Map.of("text", msg.getContent())));
                contents.add(content);
            }
        }
        requestBody.put("contents", contents);

        addSystemInstruction(requestBody, options);
        addGenerationConfig(requestBody, options);

        return executeWithModelFallback(targetModel, requestBody);
    }

    private Map<String, Object> buildRequestBody(String prompt, Map<String, Object> options) {
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("role", "user");
        userContent.put("parts", List.of(Map.of("text", prompt)));
        contents.add(userContent);
        requestBody.put("contents", contents);

        addSystemInstruction(requestBody, options);
        addGenerationConfig(requestBody, options);

        return requestBody;
    }

    private void addSystemInstruction(Map<String, Object> requestBody, Map<String, Object> options) {
        if (options != null && options.containsKey("systemInstruction") && options.get("systemInstruction") != null) {
            String sysInst = options.get("systemInstruction").toString().trim();
            if (!sysInst.isEmpty()) {
                requestBody.put("systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", sysInst))
                ));
            }
        }
    }

    private void addGenerationConfig(Map<String, Object> requestBody, Map<String, Object> options) {
        Map<String, Object> genConfig = new HashMap<>();
        if (options != null && options.containsKey("temperature")) {
            try {
                genConfig.put("temperature", Double.parseDouble(options.get("temperature").toString()));
            } catch (Exception ignored) {}
        }
        genConfig.put("maxOutputTokens", 4096);
        requestBody.put("generationConfig", genConfig);
    }

    private String executeWithModelFallback(String preferredModel, Map<String, Object> requestBody) {
        // Build fallback candidate list: preferred model first, then reliable alternatives
        List<String> candidates = new ArrayList<>();
        candidates.add(preferredModel);

        // Add current live Gemini models as fallbacks (gemini-3.5-flash confirmed working)
        String[] fallbacks = {
            "gemini-3.5-flash",
            "gemini-3.7-flash",
            "gemini-flash-latest",
            "gemini-pro-latest",
            "gemini-2.5-flash",
            "gemini-2.5-pro"
        };

        for (String fb : fallbacks) {
            if (!candidates.contains(fb)) {
                candidates.add(fb);
            }
        }

        RestClientResponseException lastException = null;

        for (String modelName : candidates) {
            String url = String.format("%s/v1beta/models/%s:generateContent?key=%s",
                    baseUrl, modelName, apiKey.trim());

            try {
                log.info("[Gemini] Calling model: {}", modelName);
                String responseJson = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                String result = extractTextFromGeminiResponse(responseJson);
                log.info("[Gemini] Successfully received response from model: {}", modelName);
                return result;

            } catch (RestClientResponseException e) {
                lastException = e;
                int status = e.getStatusCode().value();
                if (status == 404) {
                    log.warn("[Gemini] Model '{}' returned 404 (not found), trying next...", modelName);
                } else if (status == 429) {
                    log.warn("[Gemini] Model '{}' rate limited (429), trying next...", modelName);
                } else {
                    log.warn("[Gemini] Model '{}' returned HTTP {}: {}", modelName, status, e.getResponseBodyAsString());
                }
            } catch (Exception e) {
                log.error("[Gemini] Network error calling model '{}': {}", modelName, e.getMessage());
            }
        }

        // All candidates failed
        String errorMsg = lastException != null ? lastException.getResponseBodyAsString() : "Unknown error";
        log.error("[Gemini] All model candidates failed. Last error: {}", errorMsg);
        return "⚠️ Unable to reach Google Gemini API. Please check your API key and internet connection. Error: " + errorMsg;
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return "gemini-3.5-flash";
        }
        String m = model.trim().toLowerCase();

        // Map old deprecated model names to current live models
        if (m.equals("gemini-1.5-flash") || m.equals("gemini-2.0-flash") || m.equals("gemini-2.0-flash-lite") || m.equals("gemini-2.5-flash")) {
            return "gemini-3.5-flash";
        }
        if (m.equals("gemini-1.5-pro") || m.equals("gemini-pro") || m.equals("gemini-2.5-pro")) {
            return "gemini-3.5-flash";
        }

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

            // Check for blocked content
            JsonNode promptFeedback = root.path("promptFeedback");
            if (promptFeedback.has("blockReason")) {
                return "⚠️ This prompt was blocked by Google's safety filters. Reason: " + promptFeedback.get("blockReason").asText();
            }

            return "No response content generated. Raw: " + responseJson;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", responseJson, e);
            return responseJson;
        }
    }
}

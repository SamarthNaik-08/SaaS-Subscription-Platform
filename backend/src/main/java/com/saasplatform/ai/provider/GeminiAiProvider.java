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
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(20000);
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
        log.info("[Gemini Provider] Generating text with model: {}", targetModel);

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("role", "user");
        userContent.put("parts", List.of(Map.of("text", prompt)));
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
        genConfig.put("maxOutputTokens", 2048);
        requestBody.put("generationConfig", genConfig);

        return executeWithModelFallback(targetModel, requestBody);
    }

    @Override
    public String chat(List<ChatMessageDto> messages, String model, Map<String, Object> options) {
        String targetModel = normalizeModel(model);
        log.info("[Gemini Provider] Chat turn with model: {}", targetModel);

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();

        if (messages != null) {
            for (ChatMessageDto msg : messages) {
                String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "model";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.getContent()))
                ));
            }
        }
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
        genConfig.put("maxOutputTokens", 2048);
        requestBody.put("generationConfig", genConfig);

        return executeWithModelFallback(targetModel, requestBody);
    }

    private String executeWithModelFallback(String preferredModel, Map<String, Object> requestBody) {
        List<String[]> candidates = List.of(
                new String[]{"v1beta", preferredModel},
                new String[]{"v1beta", "gemini-2.5-flash"},
                new String[]{"v1beta", "gemini-2.5-pro"}
        );

        RestClientResponseException lastException = null;

        for (String[] candidate : candidates) {
            String apiVersion = candidate[0];
            String modelName = candidate[1];
            String url = String.format("%s/%s/models/%s:generateContent?key=%s", baseUrl, apiVersion, modelName, apiKey.trim());

            try {
                log.info("Executing Gemini call on {} / {}", apiVersion, modelName);
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
                    log.warn("Gemini model {} on {} returned 404, attempting fallback...", modelName, apiVersion);
                    continue;
                }
                return handleGeminiApiError(e);
            } catch (Exception e) {
                log.error("Network error calling Gemini API: {}", e.getMessage(), e);
                return "⚠️ **AI Generation Error:** Unable to reach Google Gemini API (" + e.getMessage() + "). Please check your network connection.";
            }
        }

        if (lastException != null) {
            return handleGeminiApiError(lastException);
        }

        return "⚠️ Unable to generate response from available Google Gemini models.";
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

        if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 403) {
            return String.format("""
                    ⚠️ **Google Gemini API Key Error (%s)**
                    
                    Google Gemini returned: `%s`
                    
                    > **Tip:** Google AI Studio Gemini API keys start with **`AIzaSy...`** (from https://aistudio.google.com/app/apikey).
                    > Please double-check the key in your `.env` file.
                    """, e.getStatusCode(), detail);
        }

        if (e.getStatusCode().value() == 404) {
            return "⚠️ The selected Google Gemini model is currently unavailable on Google AI Studio. Please select another model.";
        }

        return String.format("⚠️ **Google Gemini API Error (%s):** %s", e.getStatusCode(), detail);
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return "gemini-2.5-flash";
        }
        if (model.contains("gemini-2.5-flash") || model.contains("gemini-3") || model.contains("gemini-2.0") || model.contains("gemini-1.5-flash")) {
            return "gemini-2.5-flash";
        }
        if (model.contains("gemini-2.5-pro") || model.contains("gemini-1.5-pro")) {
            return "gemini-2.5-pro";
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
            return "No response content generated by Gemini.";
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", responseJson, e);
            return responseJson;
        }
    }
}

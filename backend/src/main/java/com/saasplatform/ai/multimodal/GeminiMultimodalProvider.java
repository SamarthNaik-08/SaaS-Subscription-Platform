package com.saasplatform.ai.multimodal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@Component("geminiMultimodalProvider")
public class GeminiMultimodalProvider implements AiMultimodalProvider {

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;

    public GeminiMultimodalProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(90000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

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
        String targetModel = normalizeModel(model);
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
            genConfig.put("maxOutputTokens", 8192);
            requestBody.put("generationConfig", genConfig);

            return executeWithModelFallback(targetModel, requestBody);

        } catch (Exception e) {
            log.error("[Gemini Multimodal] Error processing multimodal request: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini Multimodal Error: " + e.getMessage(), e);
        }
    }

    private String executeWithModelFallback(String preferredModel, Map<String, Object> requestBody) {
        // Build fallback list with current live models that support multimodal/vision
        List<String> candidates = new ArrayList<>();
        candidates.add(preferredModel);

        String[] fallbacks = {
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.7-flash",
            "gemini-flash-latest",
            "gemini-pro-latest"
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
                log.info("[Gemini Multimodal] Calling model: {}", modelName);
                String responseJson = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                String result = extractTextFromGeminiResponse(responseJson);
                log.info("[Gemini Multimodal] Successfully received response from model: {}", modelName);
                return result;

            } catch (RestClientResponseException e) {
                lastException = e;
                int status = e.getStatusCode().value();
                if (status == 404) {
                    log.warn("[Gemini Multimodal] Model '{}' returned 404, trying next...", modelName);
                    continue;
                }
                if (status == 429) {
                    log.warn("[Gemini Multimodal] Model '{}' rate limited (429), trying next...", modelName);
                    continue;
                }
                return handleGeminiApiError(e);
            } catch (Exception e) {
                log.error("[Gemini Multimodal] Network error calling model '{}': {}", modelName, e.getMessage());
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

        if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 403) {
            return String.format("""
                    ⚠️ **Google Gemini API Key Error (%s)**

                    Google Gemini returned: `%s`

                    > **Tip:** Check your Gemini API key in the `.env` file.
                    """, e.getStatusCode(), detail);
        }

        return String.format("⚠️ **Google Gemini API Error (%s):** %s", e.getStatusCode(), detail);
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return "gemini-2.5-flash";
        }
        String m = model.trim().toLowerCase();

        // Map old deprecated model names to current live models
        if (m.equals("gemini-1.5-flash") || m.equals("gemini-2.0-flash") || m.equals("gemini-2.0-flash-lite")) {
            return "gemini-2.5-flash";
        }
        if (m.equals("gemini-1.5-pro") || m.equals("gemini-pro")) {
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

            // Check for blocked content
            JsonNode promptFeedback = root.path("promptFeedback");
            if (promptFeedback.has("blockReason")) {
                return "⚠️ This content was blocked by Google's safety filters. Reason: " + promptFeedback.get("blockReason").asText();
            }

            return "No content was generated by Gemini for this multimodal input.";
        } catch (Exception e) {
            log.error("Failed to parse Gemini multimodal response: {}", responseJson, e);
            return responseJson;
        }
    }
}

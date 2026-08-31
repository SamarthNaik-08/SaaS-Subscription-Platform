package com.saasplatform.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.ai.dto.ChatMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("openAiProvider")
public class OpenAiProvider implements AiProvider {

    @Value("${app.ai.openai.api-key:}")
    private String apiKey;

    @Value("${app.ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("placeholder") && !apiKey.startsWith("your_");
    }

    @Override
    public String generateText(String prompt, String model, Map<String, Object> options) {
        String targetModel = normalizeModel(model);
        log.info("[OpenAI Provider] Generating text with model: {}", targetModel);

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            if (options != null && options.containsKey("systemInstruction") && options.get("systemInstruction") != null) {
                String sysInst = options.get("systemInstruction").toString().trim();
                if (!sysInst.isEmpty()) {
                    messages.add(Map.of("role", "system", "content", sysInst));
                }
            }
            messages.add(Map.of("role", "user", "content", prompt));

            return executeChatCompletions(targetModel, messages, options);
        } catch (Exception e) {
            log.error("[OpenAI Provider] Error generating text: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API Error: " + e.getMessage(), e);
        }
    }

    @Override
    public String chat(List<ChatMessageDto> messages, String model, Map<String, Object> options) {
        String targetModel = normalizeModel(model);
        log.info("[OpenAI Provider] Chat turns with model: {}", targetModel);

        try {
            List<Map<String, String>> apiMessages = new ArrayList<>();
            if (options != null && options.containsKey("systemInstruction") && options.get("systemInstruction") != null) {
                String sysInst = options.get("systemInstruction").toString().trim();
                if (!sysInst.isEmpty()) {
                    apiMessages.add(Map.of("role", "system", "content", sysInst));
                }
            }

            if (messages != null) {
                for (ChatMessageDto msg : messages) {
                    apiMessages.add(Map.of(
                            "role", "user".equalsIgnoreCase(msg.getRole()) ? "user" : "assistant",
                            "content", msg.getContent()
                    ));
                }
            }

            return executeChatCompletions(targetModel, apiMessages, options);
        } catch (Exception e) {
            log.error("[OpenAI Provider] Error during chat: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API Error: " + e.getMessage(), e);
        }
    }

    private String executeChatCompletions(String model, List<Map<String, String>> messages, Map<String, Object> options) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);

        if (options != null && options.containsKey("temperature")) {
            try {
                requestBody.put("temperature", Double.parseDouble(options.get("temperature").toString()));
            } catch (Exception ignored) {}
        }

        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

        String responseJson = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractTextFromOpenAiResponse(responseJson);
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return "gpt-4o-mini";
        }
        if (model.equalsIgnoreCase("gpt-4o")) return "gpt-4o";
        if (model.contains("claude")) return "gpt-4o-mini"; // Fallback to OpenAI standard if mapped
        return model;
    }

    private String extractTextFromOpenAiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).path("message");
                if (message.has("content")) {
                    return message.get("content").asText();
                }
            }
            return "No response content generated by OpenAI.";
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", responseJson, e);
            return responseJson;
        }
    }
}

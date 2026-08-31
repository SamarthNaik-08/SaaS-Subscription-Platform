package com.saasplatform.ai.provider.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasplatform.ai.dto.AiImageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component("openAiDalleImageProvider")
public class OpenAiDalleImageProvider implements AiImageProvider {

    @Value("${app.ai.openai.api-key:}")
    private String apiKey;

    @Value("${app.ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    @Override
    public String getProviderName() {
        return "OpenAI DALL-E";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("placeholder") && !apiKey.startsWith("your_");
    }

    @Override
    public AiImageResponse generateImage(String prompt, String model, String aspectRatio, String stylePreset, Map<String, Object> options) {
        String targetModel = (model != null && model.toLowerCase().contains("dall-e")) ? model : "dall-e-3";
        String size = "1024x1024";
        if ("16:9".equals(aspectRatio)) {
            size = "1792x1024";
        } else if ("9:16".equals(aspectRatio)) {
            size = "1024x1792";
        }

        log.info("[OpenAI DALL-E Provider] Generating image with model: {}, size: {}", targetModel, size);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", targetModel);
            requestBody.put("prompt", prompt);
            requestBody.put("n", 1);
            requestBody.put("size", size);
            requestBody.put("quality", "standard");

            String url = baseUrl.endsWith("/") ? baseUrl + "images/generations" : baseUrl + "/images/generations";

            String responseJson = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode data = root.path("data");
            if (data.isArray() && !data.isEmpty()) {
                JsonNode first = data.get(0);
                String imageUrl = first.path("url").asText();
                String revised = first.path("revised_prompt").asText(prompt);

                return AiImageResponse.builder()
                        .imageUrl(imageUrl)
                        .prompt(prompt)
                        .revisedPrompt(revised)
                        .model(targetModel)
                        .provider(getProviderName())
                        .aspectRatio(aspectRatio != null ? aspectRatio : "1:1")
                        .stylePreset(stylePreset != null ? stylePreset : "Standard")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            throw new RuntimeException("No image returned from OpenAI API");

        } catch (Exception e) {
            log.error("[OpenAI DALL-E Provider] Failed to generate image: {}", e.getMessage(), e);
            throw new RuntimeException("DALL-E Generation Error: " + e.getMessage(), e);
        }
    }
}

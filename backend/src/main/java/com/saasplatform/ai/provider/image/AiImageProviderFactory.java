package com.saasplatform.ai.provider.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiImageProviderFactory {

    private final OpenAiDalleImageProvider openAiDalleImageProvider;
    private final PollinationsAiImageProvider pollinationsAiImageProvider;
    private final MockAiImageProvider mockAiImageProvider;

    @Value("${app.ai.image.provider:auto}")
    private String configuredProvider;

    public AiImageProvider getProvider(String requestedModel) {
        String mode = configuredProvider != null ? configuredProvider.trim().toLowerCase() : "auto";

        if ("mock".equals(mode)) {
            log.info("[AiImageProviderFactory] Forced Mock provider by configuration");
            return mockAiImageProvider;
        }

        if ("openai".equals(mode) || (requestedModel != null && requestedModel.toLowerCase().contains("dall-e"))) {
            if (openAiDalleImageProvider.isAvailable()) {
                return openAiDalleImageProvider;
            }
        }

        // Auto mode: Use Pollinations for live FLUX synthesis or OpenAI if configured
        if (openAiDalleImageProvider.isAvailable() && requestedModel != null && requestedModel.toLowerCase().contains("dall-e")) {
            return openAiDalleImageProvider;
        }

        if (pollinationsAiImageProvider.isAvailable()) {
            return pollinationsAiImageProvider;
        }

        return mockAiImageProvider;
    }

    public List<Map<String, String>> getAvailableImageModels() {
        return List.of(
                Map.of("id", "flux-schnell", "name", "FLUX Schnell (High Resolution)", "provider", "Nexus FLUX"),
                Map.of("id", "flux-realism", "name", "FLUX Photorealism (Cinematic)", "provider", "Nexus FLUX"),
                Map.of("id", "dall-e-3", "name", "DALL-E 3 (OpenAI)", "provider", "OpenAI"),
                Map.of("id", "mock-image-v1", "name", "Nexus Offline Mock Image", "provider", "Mock Engine")
        );
    }
}

package com.saasplatform.ai.multimodal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiMultimodalProviderFactory {

    private final GeminiMultimodalProvider geminiMultimodalProvider;
    private final OpenAiMultimodalProvider openAiMultimodalProvider;
    private final MockMultimodalProvider mockMultimodalProvider;

    @Value("${app.ai.multimodal.provider:auto}")
    private String configuredProvider;

    public AiMultimodalProvider getProvider(String requestedModel) {
        String mode = configuredProvider != null ? configuredProvider.trim().toLowerCase() : "auto";

        if ("mock".equals(mode)) {
            log.info("[AiMultimodalProviderFactory] Forced Mock provider by configuration");
            return mockMultimodalProvider;
        }

        if ("openai".equals(mode) || (requestedModel != null && requestedModel.toLowerCase().contains("gpt"))) {
            if (openAiMultimodalProvider.isAvailable()) {
                return openAiMultimodalProvider;
            }
        }

        // Auto mode: Prioritize Gemini for rich image/PDF/code understanding
        if (geminiMultimodalProvider.isAvailable()) {
            return geminiMultimodalProvider;
        }

        if (openAiMultimodalProvider.isAvailable()) {
            return openAiMultimodalProvider;
        }

        return mockMultimodalProvider;
    }
}

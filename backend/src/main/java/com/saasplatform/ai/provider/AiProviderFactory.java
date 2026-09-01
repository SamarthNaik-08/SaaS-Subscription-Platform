package com.saasplatform.ai.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiProviderFactory {

    private final GeminiAiProvider geminiAiProvider;
    private final OpenAiProvider openAiProvider;
    private final MockAiProvider mockAiProvider;

    public AiProvider getProvider(String modelOrProvider) {
        String target = modelOrProvider != null ? modelOrProvider.toLowerCase() : "";

        // 1. If explicit Gemini model or provider requested
        if (target.contains("gemini") || target.contains("google")) {
            if (geminiAiProvider.isAvailable()) {
                return geminiAiProvider;
            }
        }

        // 2. If explicit OpenAI model or provider requested
        if (target.contains("gpt") || target.contains("openai")) {
            if (openAiProvider.isAvailable()) {
                return openAiProvider;
            }
        }

        // 3. Auto-fallback to any available live provider
        if (geminiAiProvider.isAvailable()) {
            return geminiAiProvider;
        }

        if (openAiProvider.isAvailable()) {
            return openAiProvider;
        }

        // 4. Default to MockAiProvider if no API keys are configured
        log.info("[AiProviderFactory] No live AI API keys configured. Using MockAiProvider.");
        return mockAiProvider;
    }

    public List<Map<String, String>> getAvailableModels() {
        return List.of(
                Map.of("id", "gemini-2.5-flash", "name", "Gemini 2.5 Flash (Fast & Next-Gen)", "provider", "Google", "available", String.valueOf(geminiAiProvider.isAvailable())),
                Map.of("id", "gemini-2.5-pro", "name", "Gemini 2.5 Pro (Deep Reasoning)", "provider", "Google", "available", String.valueOf(geminiAiProvider.isAvailable())),
                Map.of("id", "gpt-4o", "name", "GPT-4o (High-Throughput Multimodal)", "provider", "OpenAI", "available", String.valueOf(openAiProvider.isAvailable())),
                Map.of("id", "gpt-4o-mini", "name", "GPT-4o Mini (Cost-Optimized)", "provider", "OpenAI", "available", String.valueOf(openAiProvider.isAvailable()))
        );
    }
}

package com.saasplatform.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AiProviderFactory.class);

    private final GeminiAiProvider geminiAiProvider;
    private final OpenAiProvider openAiProvider;
    private final MockAiProvider mockAiProvider;

    public AiProviderFactory(
            GeminiAiProvider geminiAiProvider,
            OpenAiProvider openAiProvider,
            MockAiProvider mockAiProvider) {
        this.geminiAiProvider = geminiAiProvider;
        this.openAiProvider = openAiProvider;
        this.mockAiProvider = mockAiProvider;
    }

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
        List<Map<String, String>> models = new ArrayList<>();
        boolean geminiUp = geminiAiProvider.isAvailable();
        boolean openaiUp = openAiProvider.isAvailable();

        // Current live Google Gemini models
        models.add(createModelMap("gemini-2.5-flash",
                "Gemini 2.5 Flash (Fast & Smart)", "Google", geminiUp));
        models.add(createModelMap("gemini-2.5-pro",
                "Gemini 2.5 Pro (Deep Analysis)", "Google", geminiUp));
        models.add(createModelMap("gemini-3.5-flash",
                "Gemini 3.5 Flash (Latest)", "Google", geminiUp));
        models.add(createModelMap("gemini-3.7-flash",
                "Gemini 3.7 Flash (Cutting Edge)", "Google", geminiUp));

        // OpenAI models
        models.add(createModelMap("gpt-4o",
                "GPT-4o (Multimodal)", "OpenAI", openaiUp));
        models.add(createModelMap("gpt-4o-mini",
                "GPT-4o Mini (Fast)", "OpenAI", openaiUp));

        return models;
    }

    private Map<String, String> createModelMap(String id, String name, String provider, boolean available) {
        Map<String, String> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("provider", provider);
        map.put("available", String.valueOf(available));
        return map;
    }
}

package com.saasplatform.ai.service;

import com.saasplatform.ai.dto.*;
import com.saasplatform.ai.multimodal.AiMultimodalProvider;
import com.saasplatform.ai.multimodal.AiMultimodalProviderFactory;
import com.saasplatform.ai.multimodal.MultimodalAttachment;
import com.saasplatform.ai.multimodal.MultimodalFileValidator;
import com.saasplatform.ai.provider.AiProvider;
import com.saasplatform.ai.provider.AiProviderFactory;
import com.saasplatform.ai.provider.image.AiImageProvider;
import com.saasplatform.ai.provider.image.AiImageProviderFactory;
import com.saasplatform.common.enums.UsageMetric;
import com.saasplatform.usage.dto.MetricUsageDto;
import com.saasplatform.usage.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProviderFactory aiProviderFactory;
    private final AiImageProviderFactory aiImageProviderFactory;
    private final AiMultimodalProviderFactory aiMultimodalProviderFactory;
    private final MultimodalFileValidator multimodalFileValidator;
    private final UsageService usageService;

    @Transactional
    public AiGenerateResponse generateText(UUID userId, AiGenerateRequest request) {
        String prompt = request.getPrompt();
        String model = (request.getModel() != null && !request.getModel().isBlank())
                ? request.getModel() : "gemini-2.5-flash";

        String promptPreview = prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt;

        // 1. Check and record quota ATOMICALLY before invoking provider (throws 429 if exceeded)
        MetricUsageDto usage = usageService.recordUsage(
                userId,
                UsageMetric.AI_REQUEST,
                1,
                "AI Text Generation: [" + model + "] " + promptPreview
        );

        // 2. Build options map
        Map<String, Object> options = request.getParameters() != null 
                ? new HashMap<>(request.getParameters()) 
                : new HashMap<>();
        if (request.getSystemInstruction() != null) {
            options.put("systemInstruction", request.getSystemInstruction());
        }
        if (request.getTemperature() != null) {
            options.put("temperature", request.getTemperature());
        }

        // 3. Invoke Provider
        AiProvider provider = aiProviderFactory.getProvider(model);
        String completionText = provider.generateText(prompt, model, options);

        // 4. Approximate token metrics
        long promptTokens = Math.max(1, prompt.length() / 4);
        long completionTokens = Math.max(1, completionText.length() / 4);

        log.info("AI generation completed for userId={}, model={}, provider={}, promptTokens={}, completionTokens={}",
                userId, model, provider.getProviderName(), promptTokens, completionTokens);

        return AiGenerateResponse.builder()
                .text(completionText)
                .model(model)
                .provider(provider.getProviderName())
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .quotaUsage(usage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public AiGenerateResponse chat(UUID userId, AiChatRequest request) {
        List<ChatMessageDto> messages = request.getMessages();
        String model = (request.getModel() != null && !request.getModel().isBlank())
                ? request.getModel() : "gemini-2.5-flash";

        String lastMessage = (messages != null && !messages.isEmpty())
                ? messages.get(messages.size() - 1).getContent()
                : "Empty turn";
        String promptPreview = lastMessage.length() > 50 ? lastMessage.substring(0, 50) + "..." : lastMessage;

        // 1. Check and record quota ATOMICALLY before invoking provider
        MetricUsageDto usage = usageService.recordUsage(
                userId,
                UsageMetric.AI_REQUEST,
                1,
                "AI Chat Turn: [" + model + "] " + promptPreview
        );

        // 2. Build options map
        Map<String, Object> options = request.getParameters() != null 
                ? new HashMap<>(request.getParameters()) 
                : new HashMap<>();
        if (request.getSystemInstruction() != null) {
            options.put("systemInstruction", request.getSystemInstruction());
        }
        if (request.getTemperature() != null) {
            options.put("temperature", request.getTemperature());
        }

        // 3. Invoke Provider
        AiProvider provider = aiProviderFactory.getProvider(model);
        String completionText = provider.chat(messages, model, options);

        // 4. Approximate tokens
        long promptTokens = messages != null 
                ? messages.stream().mapToLong(m -> Math.max(1, m.getContent().length() / 4)).sum() 
                : 1;
        long completionTokens = Math.max(1, completionText.length() / 4);

        return AiGenerateResponse.builder()
                .text(completionText)
                .model(model)
                .provider(provider.getProviderName())
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .quotaUsage(usage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public AiImageResponse generateImage(UUID userId, AiImageRequest request) {
        String prompt = request.getPrompt().trim();
        String promptPreview = prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt;

        // 1. Check and record quota ATOMICALLY before invoking image provider (throws 429 if exceeded)
        MetricUsageDto usage = usageService.recordUsage(
                userId,
                UsageMetric.AI_REQUEST,
                1,
                "AI Image Generation: " + promptPreview
        );

        // 2. Resolve image provider
        AiImageProvider imageProvider = aiImageProviderFactory.getProvider(request.getModel());

        // 3. Invoke image provider
        AiImageResponse response = imageProvider.generateImage(
                prompt,
                request.getModel(),
                request.getAspectRatio() != null ? request.getAspectRatio() : "1:1",
                request.getStylePreset(),
                request.getParameters()
        );

        response.setQuotaUsage(usage);
        response.setTimestamp(LocalDateTime.now());

        log.info("AI image generation completed for userId={}, provider={}, model={}",
                userId, response.getProvider(), response.getModel());

        return response;
    }

    @Transactional
    public AiGenerateResponse processMultimodal(UUID userId, String prompt, List<MultipartFile> files, String model, Map<String, Object> options) {
        String resolvedPrompt = (prompt != null && !prompt.isBlank()) ? prompt.trim() : "Analyze the attached file(s) in detail.";
        String promptPreview = resolvedPrompt.length() > 50 ? resolvedPrompt.substring(0, 50) + "..." : resolvedPrompt;

        // 1. Strict File Validation & Memory Transformation (No disk storage)
        List<MultimodalAttachment> attachments = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null) {
                    attachments.add(multimodalFileValidator.validateAndConvert(file));
                }
            }
        }

        // 2. Atomic Quota Consumption (Throws 429 if limit reached)
        MetricUsageDto usage = usageService.recordUsage(
                userId,
                UsageMetric.AI_REQUEST,
                1,
                "Multimodal Analysis: " + promptPreview
        );

        // 3. Resolve Multimodal Provider
        AiMultimodalProvider multimodalProvider = aiMultimodalProviderFactory.getProvider(model);

        // 4. Process Multimodal Inference
        String completionText = multimodalProvider.processMultimodal(resolvedPrompt, attachments, model, options != null ? options : Map.of());

        // 5. Approximate token metrics
        long promptTokens = Math.max(1, resolvedPrompt.length() / 4) + (attachments.size() * 256L);
        long completionTokens = Math.max(1, completionText.length() / 4);

        log.info("Multimodal inference completed for userId={}, provider={}, attachments={}, promptTokens={}, completionTokens={}",
                userId, multimodalProvider.getProviderName(), attachments.size(), promptTokens, completionTokens);

        return AiGenerateResponse.builder()
                .text(completionText)
                .model(model != null ? model : "gemini-2.5-flash")
                .provider(multimodalProvider.getProviderName())
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .quotaUsage(usage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public List<Map<String, String>> getAvailableModels() {
        return aiProviderFactory.getAvailableModels();
    }

    public List<Map<String, String>> getAvailableImageModels() {
        return aiImageProviderFactory.getAvailableImageModels();
    }
}

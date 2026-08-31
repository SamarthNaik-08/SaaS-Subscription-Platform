package com.saasplatform.ai.provider.image;

import com.saasplatform.ai.dto.AiImageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component("pollinationsAiImageProvider")
public class PollinationsAiImageProvider implements AiImageProvider {

    @Override
    public String getProviderName() {
        return "Nexus FLUX Engine";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiImageResponse generateImage(String prompt, String model, String aspectRatio, String stylePreset, Map<String, Object> options) {
        log.info("[Pollinations Engine] Synthesizing image prompt: {}, ratio: {}, style: {}", prompt, aspectRatio, stylePreset);

        String ratio = (aspectRatio != null && !aspectRatio.isBlank()) ? aspectRatio : "1:1";
        String style = (stylePreset != null && !stylePreset.isBlank()) ? stylePreset : "Cinematic";
        String resolvedModel = (model != null && !model.isBlank()) ? model : "flux-schnell";

        int width = 1024;
        int height = 1024;
        if ("16:9".equals(ratio)) {
            width = 1280;
            height = 720;
        } else if ("9:16".equals(ratio)) {
            width = 720;
            height = 1280;
        } else if ("4:3".equals(ratio)) {
            width = 1024;
            height = 768;
        }

        String fullPrompt = prompt;
        if (stylePreset != null && !stylePreset.equalsIgnoreCase("Default") && !stylePreset.isBlank()) {
            fullPrompt += ", in " + stylePreset + " aesthetic, 8k resolution, highly detailed, masterwork";
        }

        long seed = Math.abs(prompt.hashCode() ^ System.currentTimeMillis() % 1000000L);
        String encodedPrompt = URLEncoder.encode(fullPrompt, StandardCharsets.UTF_8);

        String imageUrl = String.format(
                "https://image.pollinations.ai/prompt/%s?width=%d&height=%d&seed=%d&nologo=true&enhance=true",
                encodedPrompt, width, height, seed
        );

        return AiImageResponse.builder()
                .imageUrl(imageUrl)
                .prompt(prompt)
                .revisedPrompt(fullPrompt)
                .model(resolvedModel)
                .provider(getProviderName())
                .aspectRatio(ratio)
                .stylePreset(style)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

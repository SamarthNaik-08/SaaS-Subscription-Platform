package com.saasplatform.ai.provider.image;

import com.saasplatform.ai.dto.AiImageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component("mockAiImageProvider")
public class MockAiImageProvider implements AiImageProvider {

    @Override
    public String getProviderName() {
        return "Nexus Mock Image Engine";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiImageResponse generateImage(String prompt, String model, String aspectRatio, String stylePreset, Map<String, Object> options) {
        log.info("[Mock Image Provider] Generating mock image for prompt: {}, model: {}, aspectRatio: {}", prompt, model, aspectRatio);

        String ratio = (aspectRatio != null && !aspectRatio.isBlank()) ? aspectRatio : "1:1";
        String style = (stylePreset != null && !stylePreset.isBlank()) ? stylePreset : "Cinematic";
        String resolvedModel = (model != null && !model.isBlank()) ? model : "mock-image-v1";

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

        // Generate a deterministic styled SVG data URI representation
        String escapedPrompt = prompt.replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        if (escapedPrompt.length() > 80) {
            escapedPrompt = escapedPrompt.substring(0, 80) + "...";
        }

        String svg = String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" width=\"%d\" height=\"%d\">" +
                "<defs>" +
                "<linearGradient id=\"grad\" x1=\"0%%\" y1=\"0%%\" x2=\"100%%\" y2=\"100%%\">" +
                "<stop offset=\"0%%\" stop-color=\"#0f172a\" />" +
                "<stop offset=\"50%%\" stop-color=\"#312e81\" />" +
                "<stop offset=\"100%%\" stop-color=\"#4338ca\" />" +
                "</linearGradient>" +
                "<linearGradient id=\"accent\" x1=\"0%%\" y1=\"0%%\" x2=\"100%%\" y2=\"0%%\">" +
                "<stop offset=\"0%%\" stop-color=\"#818cf8\" />" +
                "<stop offset=\"100%%\" stop-color=\"#c084fc\" />" +
                "</linearGradient>" +
                "</defs>" +
                "<rect width=\"%d\" height=\"%d\" fill=\"url(#grad)\" />" +
                "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"#6366f1\" opacity=\"0.2\" />" +
                "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"#a855f7\" opacity=\"0.15\" />" +
                "<rect x=\"40\" y=\"40\" width=\"%d\" height=\"%d\" rx=\"24\" fill=\"none\" stroke=\"url(#accent)\" stroke-width=\"2\" stroke-dasharray=\"12 6\" opacity=\"0.6\" />" +
                "<text x=\"50%%\" y=\"42%%\" text-anchor=\"middle\" fill=\"#e0e7ff\" font-family=\"system-ui, sans-serif\" font-size=\"28\" font-weight=\"bold\">🎨 Nexus AI Image Generator</text>" +
                "<text x=\"50%%\" y=\"50%%\" text-anchor=\"middle\" fill=\"#f8fafc\" font-family=\"system-ui, sans-serif\" font-size=\"20\" font-style=\"italic\">“%s”</text>" +
                "<text x=\"50%%\" y=\"58%%\" text-anchor=\"middle\" fill=\"#94a3b8\" font-family=\"monospace\" font-size=\"14\">Style: %s • Ratio: %s • %dx%d</text>" +
                "<rect x=\"%d\" y=\"%d\" width=\"220\" height=\"36\" rx=\"18\" fill=\"#1e1b4b\" stroke=\"#6366f1\" stroke-width=\"1\" />" +
                "<text x=\"50%%\" y=\"%d\" text-anchor=\"middle\" fill=\"#a5b4fc\" font-family=\"system-ui, sans-serif\" font-size=\"13\" font-weight=\"600\">Deterministic Mock Image</text>" +
                "</svg>",
                width, height, width, height,
                width, height,
                width / 3, height / 3, Math.min(width, height) / 3,
                (width * 2) / 3, (height * 2) / 3, Math.min(width, height) / 4,
                width - 80, height - 80,
                escapedPrompt,
                style, ratio, width, height,
                (width / 2) - 110, height - 90,
                height - 67
        );

        String encodedSvg = "data:image/svg+xml;charset=utf-8," + URLEncoder.encode(svg, StandardCharsets.UTF_8);

        return AiImageResponse.builder()
                .imageUrl(encodedSvg)
                .prompt(prompt)
                .revisedPrompt("High quality visual rendering of " + prompt + " in " + style + " style.")
                .model(resolvedModel)
                .provider(getProviderName())
                .aspectRatio(ratio)
                .stylePreset(style)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

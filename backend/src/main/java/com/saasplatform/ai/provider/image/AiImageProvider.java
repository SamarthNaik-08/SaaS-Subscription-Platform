package com.saasplatform.ai.provider.image;

import com.saasplatform.ai.dto.AiImageResponse;

import java.util.Map;

public interface AiImageProvider {

    String getProviderName();

    boolean isAvailable();

    AiImageResponse generateImage(String prompt, String model, String aspectRatio, String stylePreset, Map<String, Object> options);
}

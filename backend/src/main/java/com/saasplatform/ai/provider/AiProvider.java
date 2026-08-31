package com.saasplatform.ai.provider;

import com.saasplatform.ai.dto.ChatMessageDto;

import java.util.List;
import java.util.Map;

public interface AiProvider {

    String getProviderName();

    boolean isAvailable();

    String generateText(String prompt, String model, Map<String, Object> options);

    String chat(List<ChatMessageDto> messages, String model, Map<String, Object> options);
}

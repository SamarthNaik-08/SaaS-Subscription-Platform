package com.saasplatform.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    @NotEmpty(message = "Messages list cannot be empty")
    private List<ChatMessageDto> messages;

    private String model;

    private String systemInstruction;

    private Double temperature;

    private Map<String, Object> parameters;
}

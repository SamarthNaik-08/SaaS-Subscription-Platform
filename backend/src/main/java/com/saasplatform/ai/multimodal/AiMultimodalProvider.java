package com.saasplatform.ai.multimodal;

import java.util.List;
import java.util.Map;

public interface AiMultimodalProvider {

    String getProviderName();

    boolean isAvailable();

    String processMultimodal(String prompt, List<MultimodalAttachment> attachments, String model, Map<String, Object> options);
}

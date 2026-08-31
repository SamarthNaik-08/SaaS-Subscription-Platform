package com.saasplatform.ai.multimodal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultimodalAttachment {

    private String fileName;

    private String contentType;

    private long fileSizeBytes;

    private byte[] bytes;

    private String base64Data;

    private String textContent;

    private boolean image;

    private boolean pdf;

    private boolean textDocument;
}

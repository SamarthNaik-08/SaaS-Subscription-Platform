package com.saasplatform.ai.multimodal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("mockMultimodalProvider")
public class MockMultimodalProvider implements AiMultimodalProvider {

    @Override
    public String getProviderName() {
        return "Nexus Mock Multimodal Engine";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String processMultimodal(String prompt, List<MultimodalAttachment> attachments, String model, Map<String, Object> options) {
        log.info("[Mock Multimodal Engine] Processing {} attachments with prompt: {}", 
                attachments != null ? attachments.size() : 0, prompt);

        StringBuilder sb = new StringBuilder();
        String userPrompt = (prompt != null && !prompt.isBlank()) ? prompt : "Summarize and analyze attached context.";

        sb.append(String.format("### 🔍 Multimodal Analysis: \"%s\"\n\n", userPrompt));

        if (attachments == null || attachments.isEmpty()) {
            sb.append("No files were attached. Please attach an image, PDF, or code file for multimodal analysis.");
            return sb.toString();
        }

        for (int i = 0; i < attachments.size(); i++) {
            MultimodalAttachment att = attachments.get(i);
            sb.append(String.format("#### 📄 Attachment %d: `%s` (%s, %.1f KB)\n\n", 
                    i + 1, att.getFileName(), att.getContentType(), att.getFileSizeBytes() / 1024.0));

            if (att.isImage()) {
                sb.append("""
                        * **Visual Type:** Raster/Vector Graphic Asset
                        * **Content Overview:** The image contains visual composition elements with balanced contrast, color hierarchy, and structured layout.
                        * **Key Observations:**
                          1. Clear focal hierarchy and foreground-to-background subject separation.
                          2. High fidelity visual elements suitable for web and mobile interfaces.
                          3. Consistent color palette and aesthetic alignment with consumer UI standards.
                        """);
            } else if (att.isPdf()) {
                sb.append("""
                        * **Document Type:** PDF Document
                        * **Executive Summary:**
                          1. **Core Subject:** Outlines operational guidelines, structured specifications, and system requirements.
                          2. **Key Findings:** Focuses on deterministic reliability, security compliance, and performance metrics.
                          3. **Actionable Recommendations:** Implement automated monitoring and continuous validation pipelines.
                        """);
            } else if (att.isTextDocument()) {
                String content = att.getTextContent() != null ? att.getTextContent() : "";
                String lower = att.getFileName().toLowerCase();

                if (lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".js") || lower.endsWith(".ts")) {
                    sb.append(String.format("""
                            * **Code Inspection & Architecture Review:**
                              * **Language/Dialect:** %s
                              * **Lines Analyzed:** ~%d lines
                            * **Key Findings & Recommendations:**
                              1. **Code Structure:** Clean separation of concerns with modern syntax conventions.
                              2. **Quality & Safety:** Ensure complete error handling for edge-case I/O exceptions.
                              3. **Performance:** Verified thread-safety and optimized memory allocation.
                            """, 
                            att.getContentType(), 
                            Math.max(1, content.split("\r\n|\r|\n").length)
                    ));
                } else if (lower.endsWith(".csv") || lower.endsWith(".json")) {
                    sb.append("""
                            * **Data Schema & Metric Analysis:**
                              1. **Data Structure:** Well-formed structured tabular records.
                              2. **Trends Observed:** Consistent distribution across primary categorical dimensions with zero corrupt rows.
                              3. **Insights:** Ready for analytical aggregation or downstream dashboard visualization.
                            """);
                } else {
                    sb.append(String.format("""
                            * **Document Digest:**
                              * Processed text document with %d characters.
                              * Key concepts summarized and structured for actionable review.
                            """, content.length()));
                }
            }
            sb.append("\n---\n\n");
        }

        sb.append("> 💡 *Note: Processed via Nexus Mock Multimodal Engine. To connect live Google Gemini 2.0 / OpenAI Vision models, configure `GEMINI_API_KEY` or `OPENAI_API_KEY` in `.env`.*");

        return sb.toString();
    }
}

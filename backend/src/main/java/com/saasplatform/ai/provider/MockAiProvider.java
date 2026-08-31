package com.saasplatform.ai.provider;

import com.saasplatform.ai.dto.ChatMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("mockAiProvider")
public class MockAiProvider implements AiProvider {

    @Override
    public String getProviderName() {
        return "Nexus Built-in AI (Offline Mode)";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String generateText(String prompt, String model, Map<String, Object> options) {
        log.info("[MockAiProvider] Generating response for model={}, prompt preview={}",
                model != null ? model : "gemini-1.5-flash",
                prompt.length() > 60 ? prompt.substring(0, 60) + "..." : prompt);

        String lower = prompt.toLowerCase().trim();

        // 1. Specific comprehensive topic: India
        if (lower.contains("india") || lower.contains("bharat")) {
            return """
                    ### Overview of India (Republic of India)
                    
                    **India** is a vast and diverse country in South Asia, bounded by the Indian Ocean on the south, the Arabian Sea on the southwest, and the Bay of Bengal on the southeast. It is the world's most populous nation and the seventh-largest country by land area.
                    
                    ---
                    
                    #### 🏛️ Key Facts & Geography
                    * **Capital:** New Delhi
                    * **Financial Capital:** Mumbai
                    * **Official Languages:** Hindi and English (with 22 officially recognized regional languages)
                    * **Currency:** Indian Rupee (INR / ₹)
                    * **Government:** Federal Parliamentary Constitutional Republic
                    
                    ---
                    
                    #### 📜 History & Cultural Heritage
                    * **Ancient Civilizations:** Home to the Indus Valley Civilization (one of the world's oldest urban civilizations) and the Vedic era.
                    * **Religions Founded:** Birthplace of four major world religions — Hinduism, Buddhism, Jainism, and Sikhism.
                    * **Monuments & UNESCO Sites:** Features iconic landmarks including the Taj Mahal, Red Fort, Qutub Minar, Ajanta & Ellora Caves, and Hampi.
                    
                    ---
                    
                    #### 🚀 Economy, Technology & Space
                    * **Economy:** 5th largest economy in the world by nominal GDP and 3rd largest by Purchasing Power Parity (PPP).
                    * **Tech & Innovation:** Global powerhouse in IT services, software development, SaaS, and pharmaceutical innovation (the "Pharmacy of the World").
                    * **Space Exploration:** ISRO (Indian Space Research Organisation) achieved historic milestones including the Chandrayaan-3 lunar landing at the Moon's South Pole and the Aditya-L1 solar mission.
                    
                    ---
                    > 💡 *Note: Running on Nexus Built-in Engine. To connect live Google Gemini or OpenAI models, configure `GEMINI_API_KEY` or `OPENAI_API_KEY` in your `.env` or `application.yml` file.*
                    """;
        }

        // 2. Coding and technical requests
        if (lower.contains("code") || lower.contains("python") || lower.contains("java") || lower.contains("react") || lower.contains("function") || lower.contains("javascript")) {
            return """
                    ### Solution & Implementation
                    
                    Here is a clean, modular implementation designed for production reliability:
                    
                    ```javascript
                    // SaaS Analytics & Token Quota Stream Processor
                    export async function processAiRequest({ prompt, model, maxTokens = 1000 }) {
                      const startTime = performance.now();
                      
                      try {
                        const response = await fetch('/api/v1/ai/generate', {
                          method: 'POST',
                          headers: { 'Content-Type': 'application/json' },
                          body: JSON.stringify({ prompt, model, parameters: { maxTokens } })
                        });
                        
                        if (!response.ok) {
                          throw new Error(`Inference failed with HTTP status ${response.status}`);
                        }
                        
                        const result = await response.json();
                        const latency = ((performance.now() - startTime) / 1000).toFixed(2);
                        
                        return { data: result.data, latency };
                      } catch (error) {
                        console.error('AI Request Error:', error);
                        throw error;
                      }
                    }
                    ```
                    
                    #### Key Advantages:
                    1. **Deterministic Latency Tracking:** Measures network round-trip time precisely using `performance.now()`.
                    2. **Full Error Isolation:** Captures upstream quota rejections gracefully.
                    3. **Modern Async/Await:** Clean, readable, and promise-safe.
                    
                    ---
                    > 💡 *Tip: Set your `GEMINI_API_KEY` or `OPENAI_API_KEY` in `.env` to execute live real-time code generation for any language.*
                    """;
        }

        // 3. Summarization & Analysis requests
        if (lower.contains("summary") || lower.contains("summarize") || lower.contains("explain")) {
            return String.format("""
                    ### Detailed Summary & Analysis
                    
                    **Topic:** *"%s"*
                    
                    #### 1. Executive Synopsis
                    The core subject revolves around optimizing capabilities, streamlining workflows, and delivering actionable value.
                    
                    #### 2. Key Pillars & Findings
                    * **Reliability & Scalability:** Ensuring infrastructure and data models scale efficiently under dynamic workloads.
                    * **User Experience & Responsiveness:** Minimizing latency with immediate feedback loops and high usability.
                    * **Security & Isolation:** Enforcing atomic transaction isolation and authenticated tier verification.
                    
                    #### 3. Strategic Recommendations
                    * Prioritize automated metering and real-time observability.
                    * Leverage distributed caching and token budgeting for optimal resource efficiency.
                    
                    ---
                    > 💡 *To unlock live real-time AI generation across all topics, set `GEMINI_API_KEY` or `OPENAI_API_KEY` in your `.env`.*
                    """, prompt.length() > 60 ? prompt.substring(0, 60) + "..." : prompt);
        }

        // 4. Default dynamic intelligent response
        return String.format("""
                ### Information & Insights: %s
                
                Thank you for your prompt! Here is the relevant information regarding your query:
                
                * **Core Analysis:** Your inquiry regarding *"%s"* requires evaluating key foundational principles, contextual background, and practical applications.
                * **Key Dimensions:**
                  1. **Context & Relevance:** Modern information systems process natural language inquiries by synthesizing factual context with multi-turn reasoning.
                  2. **Best Practices:** Structure inputs with clear context, specific constraints, and desired output formats for optimal results.
                  3. **Exploration:** You can explore deep dives, compare architectural options, or refine specific sub-topics directly in this AI Studio.
                
                ---
                > 💡 *Note: To connect live Google Gemini (`gemini-1.5-flash`, `gemini-1.5-pro`) or OpenAI (`gpt-4o`) models with real-time internet knowledge, add your `GEMINI_API_KEY` or `OPENAI_API_KEY` in `application.yml` or `.env` file.*
                """,
                prompt.length() > 30 ? prompt.substring(0, 30) : prompt,
                prompt.length() > 80 ? prompt.substring(0, 80) + "..." : prompt
        );
    }

    @Override
    public String chat(List<ChatMessageDto> messages, String model, Map<String, Object> options) {
        if (messages == null || messages.isEmpty()) {
            return "Hello! How can I assist you with your AI questions or tasks today?";
        }
        ChatMessageDto lastMessage = messages.get(messages.size() - 1);
        return generateText(lastMessage.getContent(), model, options);
    }
}

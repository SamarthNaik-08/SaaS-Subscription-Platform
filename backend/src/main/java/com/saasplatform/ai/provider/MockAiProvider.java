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
        return "Nexus AI Engine";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String generateText(String prompt, String model, Map<String, Object> options) {
        log.info("[MockAiProvider] Generating high-quality response for prompt: {}", prompt);
        return synthesizeKnowledgeResponse(prompt);
    }

    @Override
    public String chat(List<ChatMessageDto> messages, String model, Map<String, Object> options) {
        String lastMessage = (messages != null && !messages.isEmpty())
                ? messages.get(messages.size() - 1).getContent()
                : "Hello";
        return synthesizeKnowledgeResponse(lastMessage);
    }

    private String synthesizeKnowledgeResponse(String prompt) {
        String p = prompt != null ? prompt.toLowerCase().trim() : "";

        // 1. JAVA
        if (p.contains("what is java") || (p.contains("java") && !p.contains("javascript"))) {
            return """
                    ### What is Java?
                    
                    **Java** is a high-level, class-based, object-oriented, and platform-independent programming language created by **Sun Microsystems** (released in 1995 by James Gosling) and currently maintained by **Oracle Corporation**.
                    
                    ---
                    
                    #### 🌟 Core Principles & Features
                    
                    1. **Platform Independence (WORA - Write Once, Run Anywhere):**
                       * Java code compiles into platform-independent **Bytecode** (`.class` files).
                       * The **Java Virtual Machine (JVM)** interprets and JIT-compiles this bytecode into machine-native instructions on any operating system (Windows, Linux, macOS).
                    
                    2. **Object-Oriented (OOP):**
                       * Everything in Java revolves around Classes and Objects, adhering strictly to Abstraction, Encapsulation, Inheritance, and Polymorphism.
                    
                    3. **Automatic Memory Management:**
                       * Java features a built-in **Garbage Collector (GC)** that automatically reclaims unreferenced heap memory, eliminating manual pointer manipulation and memory leaks.
                    
                    4. **Strong Typing & Security:**
                       * Strict compile-time and runtime type checking, bytecode verification, and sandboxed execution ensure enterprise-grade security.
                    
                    5. **Rich Standard Library & Ecosystem:**
                       * Massive ecosystem supporting Enterprise Web Applications (**Spring Boot, Quarkus, Jakarta EE**), Android Mobile Development, and Distributed Big Data Processing (**Apache Spark, Kafka, Hadoop**).
                    
                    ---
                    
                    #### 💻 Example Code
                    
                    ```java
                    public class HelloWorld {
                        public static void main(String[] args) {
                            System.out.println("Hello, World from Java!");
                        }
                    }
                    ```
                    """;
        }

        // 2. OOP (Object-Oriented Programming)
        if (p.contains("oop") || p.contains("object oriented") || p.contains("object-oriented")) {
            return """
                    ### Object-Oriented Programming (OOP)
                    
                    **Object-Oriented Programming (OOP)** is a programming paradigm centered around **Objects** (data structures containing attributes/fields and behaviors/methods) rather than functions and sequential logic.
                    
                    ---
                    
                    #### 🏛️ The 4 Fundamental Pillars of OOP
                    
                    1. **Encapsulation:**
                       * Bundling data (variables) and methods that operate on that data into a single unit (Class) while restricting direct external access using access modifiers (`private`, `protected`, `public`).
                    
                    2. **Abstraction:**
                       * Hiding internal implementation complexities and exposing only essential interfaces to the consumer (e.g., using `interface` and `abstract class`).
                    
                    3. **Inheritance:**
                       * Mechanism where a child class acquires attributes and behaviors from a parent class, promoting code reusability and hierarchical classification (`extends` keyword).
                    
                    4. **Polymorphism ("Many Forms"):**
                       * **Compile-Time (Method Overloading):** Multiple methods with the same name but different parameters.
                       * **Runtime (Method Overriding):** A subclass providing a specific implementation of a method defined in its parent class (`@Override`).
                    
                    ---
                    
                    #### 💡 Real-World Benefits
                    * **Modularity:** Isolated components make large codebases easy to maintain and test.
                    * **Extensibility:** New features can be added via subclassing without modifying existing code (Open/Closed Principle).
                    """;
        }

        // 3. REACT
        if (p.contains("react") || p.contains("reactjs") || p.contains("react.js")) {
            return """
                    ### What is React?
                    
                    **React** (also known as React.js) is an open-source, component-driven JavaScript/TypeScript front-end library developed by **Meta (Facebook)** for building dynamic and interactive user interfaces for single-page web applications (SPAs).
                    
                    ---
                    
                    #### ⚡ Key Architectural Concepts
                    
                    1. **Component-Based Architecture:**
                       * UIs are split into independent, reusable, and self-contained components (e.g., `<Navbar />`, `<Button />`, `<ChatContainer />`).
                    
                    2. **Virtual DOM (VDOM) & Reconciliation:**
                       * React maintains an in-memory lightweight representation of the DOM. When state changes occur, React computes the minimal difference ("diffing algorithm") and efficiently updates only the changed DOM nodes.
                    
                    3. **Declarative UI:**
                       * You describe *what* the UI should look like for each state, and React automatically updates the view when the state changes.
                    
                    4. **React Hooks:**
                       * Powerful functional primitives like `useState`, `useEffect`, `useContext`, `useMemo`, and `useCallback` manage state and lifecycle without class components.
                    
                    ---
                    
                    #### 🚀 Example Component
                    
                    ```jsx
                    import React, { useState } from 'react';
                    
                    export function Counter() {
                      const [count, setCount] = useState(0);
                      return (
                        <button onClick={() => setCount(prev => prev + 1)}>
                          Clicks: {count}
                        </button>
                      );
                    }
                    ```
                    """;
        }

        // 4. PYTHON
        if (p.contains("python")) {
            return """
                    ### What is Python?
                    
                    **Python** is a high-level, interpreted, dynamically typed, and general-purpose programming language designed by **Guido van Rossum** in 1991. It emphasizes code readability and simplicity through clean syntax and significant indentation.
                    
                    ---
                    
                    #### 🎯 Primary Applications
                    1. **Artificial Intelligence & Machine Learning:** PyTorch, TensorFlow, Scikit-learn, HuggingFace.
                    2. **Data Science & Analytics:** Pandas, NumPy, Matplotlib, Polars.
                    3. **Web Backends:** FastAPI, Django, Flask.
                    4. **Automation & Scripting:** Web scraping, DevOps pipelines, task orchestration.
                    """;
        }

        // 5. SPRING BOOT
        if (p.contains("spring") || p.contains("spring boot")) {
            return """
                    ### What is Spring Boot?
                    
                    **Spring Boot** is an open-source Java framework developed by Pivotal/VMware used to build stand-alone, production-ready enterprise Spring applications with minimal configuration.
                    
                    ---
                    
                    #### 🚀 Core Advantages
                    1. **Opinionated Auto-Configuration:** Automatically configures database connections, security filters, and web servers based on classpath dependencies.
                    2. **Embedded Servers:** Packages Tomcat, Jetty, or Undertow directly inside the runnable JAR (no standalone WAR deployment required).
                    3. **Starter Dependencies:** Curated Maven/Gradle dependency bundles (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`).
                    4. **Production-Ready Actuator:** Built-in health checks, metrics, and application telemetry endpoints (`/actuator/health`).
                    """;
        }

        // 6. GENERAL CONVERSATION / FALLBACK KNOWLEDGE
        return String.format("""
                ### Nexus AI Response
                
                Here is a structured explanation addressing your question:
                
                ---
                
                #### 📌 Key Insights & Overview
                * **Topic:** %s
                * **Analysis:** Based on standard domain knowledge, this involves foundational principles of modern software architecture, logical design, and best engineering practices.
                
                ---
                
                #### 💡 Core Principles
                1. **Structured Clarity:** Breaking down complex problems into modular, maintainable, and verifiable components.
                2. **Industry Best Practices:** Adhering to standards, safety protocols, and modern operational frameworks.
                3. **Practical Application:** Designing scalable implementations tailored for production environments.
                
                ---
                *Let me know if you would like code examples, deep architectural breakdowns, or further explanations!*
                """, prompt != null && !prompt.isBlank() ? prompt : "General Inquiry");
    }
}

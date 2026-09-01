package com.saasplatform.ai.provider;

import com.saasplatform.ai.dto.ChatMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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

        // Build ordered topic matchers: first match wins
        LinkedHashMap<java.util.function.Predicate<String>, Supplier<String>> knowledgeBase = new LinkedHashMap<>();

        // === GREETINGS ===
        knowledgeBase.put(
            q -> q.matches("^(hi|hello|hey|howdy|good morning|good evening|good afternoon)\\b.*"),
            () -> """
                Hello! 👋 I'm **Nexus AI**, your intelligent assistant. I can help you with:

                * **Programming & Technology** — Java, Python, React, Spring Boot, databases, cloud, and more
                * **General Knowledge** — History, geography, science, mathematics, and current affairs
                * **Interview Preparation** — Technical questions, coding challenges, and career guidance
                * **Writing & Analysis** — Explanations, summaries, comparisons, and structured research

                Feel free to ask me anything! 🚀
                """
        );

        // === PROGRAMMING LANGUAGES & FRAMEWORKS ===

        // JAVA (but not JavaScript)
        knowledgeBase.put(
            q -> (q.contains("what is java") || q.contains("explain java") || q.contains("about java")) && !q.contains("javascript"),
            () -> """
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
                """
        );

        // JAVA INTERVIEW QUESTIONS
        knowledgeBase.put(
            q -> q.contains("java") && (q.contains("interview") || q.contains("question")),
            () -> """
                ### Top Java Developer Interview Questions

                ---

                #### 🔹 Core Java

                1. **What is the difference between JDK, JRE, and JVM?**
                   * **JDK** (Java Development Kit): Full development package including compiler (`javac`), debugger, and libraries.
                   * **JRE** (Java Runtime Environment): Runtime libraries and JVM needed to run Java programs.
                   * **JVM** (Java Virtual Machine): The engine that executes Java bytecode on any platform.

                2. **What is the difference between `==` and `.equals()` in Java?**
                   * `==` compares **reference equality** (do both variables point to the same object in memory?).
                   * `.equals()` compares **value equality** (do the objects contain the same data?).

                3. **What is the difference between `ArrayList` and `LinkedList`?**
                   * `ArrayList`: Backed by a dynamic array, fast random access O(1), slow insertion/deletion in the middle O(n).
                   * `LinkedList`: Backed by a doubly linked list, fast insertion/deletion O(1), slow random access O(n).

                4. **Explain `final`, `finally`, and `finalize`.**
                   * `final`: Keyword to declare constants, prevent method overriding, or prevent class inheritance.
                   * `finally`: Block that always executes after try/catch (used for cleanup like closing resources).
                   * `finalize()`: Method called by the Garbage Collector before destroying an object (deprecated since Java 9).

                5. **What is multithreading in Java?**
                   * Java supports concurrent execution of two or more threads using `Thread` class or `Runnable` interface.
                   * Modern Java uses `ExecutorService`, `CompletableFuture`, and virtual threads (Project Loom in Java 21+).

                ---

                #### 🔹 Advanced Java

                6. **What are Java Streams?**
                   * A functional-style API for processing collections: `list.stream().filter(x -> x > 5).map(x -> x * 2).collect(Collectors.toList())`.

                7. **Explain the Singleton Design Pattern.**
                   * Ensures only one instance of a class exists. Implemented using private constructor and static `getInstance()` method.

                8. **What is Spring Boot and why use it?**
                   * Framework for building production-ready Java apps with auto-configuration, embedded servers, and minimal boilerplate.

                9. **Explain Dependency Injection (DI).**
                   * A design pattern where objects receive their dependencies from an external source (like Spring IoC container) rather than creating them internally.

                10. **What is the difference between REST and SOAP?**
                    * **REST**: Lightweight, uses HTTP methods (GET/POST/PUT/DELETE), returns JSON/XML.
                    * **SOAP**: Protocol-based, uses XML, has built-in WS-Security, more rigid structure.
                """
        );

        // OOP
        knowledgeBase.put(
            q -> q.contains("oop") || q.contains("object oriented") || q.contains("object-oriented"),
            () -> """
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
                """
        );

        // REACT
        knowledgeBase.put(
            q -> q.contains("react") || q.contains("reactjs") || q.contains("react.js"),
            () -> """
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
                """
        );

        // PYTHON
        knowledgeBase.put(
            q -> q.contains("python"),
            () -> """
                ### What is Python?

                **Python** is a high-level, interpreted, dynamically typed, and general-purpose programming language designed by **Guido van Rossum** in 1991. It emphasizes code readability and simplicity through clean syntax and significant indentation.

                ---

                #### 🎯 Primary Applications
                1. **Artificial Intelligence & Machine Learning:** PyTorch, TensorFlow, Scikit-learn, HuggingFace Transformers.
                2. **Data Science & Analytics:** Pandas, NumPy, Matplotlib, Polars, Jupyter Notebooks.
                3. **Web Backends:** FastAPI, Django, Flask.
                4. **Automation & Scripting:** Web scraping (BeautifulSoup, Selenium), DevOps pipelines, task orchestration.
                5. **Scientific Computing:** SciPy, SymPy, and large-scale simulations.

                ---

                #### 💻 Example Code

                ```python
                def greet(name):
                    return f"Hello, {name}! Welcome to Python."

                print(greet("World"))
                ```
                """
        );

        // JAVASCRIPT
        knowledgeBase.put(
            q -> q.contains("javascript") || q.contains("js") && (q.contains("what") || q.contains("explain")),
            () -> """
                ### What is JavaScript?

                **JavaScript** is a high-level, interpreted, dynamically typed programming language that is one of the three core technologies of the World Wide Web (alongside HTML and CSS). Originally designed by **Brendan Eich** at Netscape in 1995.

                ---

                #### ⚡ Key Features

                1. **Multi-Paradigm:** Supports object-oriented, functional, and event-driven programming.
                2. **Client & Server:** Runs in browsers (Chrome V8, Firefox SpiderMonkey) and on servers via **Node.js**.
                3. **Asynchronous & Non-Blocking:** Utilizes event loops, Promises, and `async/await` for non-blocking I/O.
                4. **Ecosystem:** NPM (world's largest package registry), React, Angular, Vue, Express, Next.js.

                ---

                #### 💻 Example

                ```javascript
                const greet = (name) => `Hello, ${name}!`;
                console.log(greet("World"));
                ```
                """
        );

        // SPRING BOOT
        knowledgeBase.put(
            q -> q.contains("spring boot") || (q.contains("spring") && q.contains("framework")),
            () -> """
                ### What is Spring Boot?

                **Spring Boot** is an open-source Java framework developed by Pivotal/VMware used to build stand-alone, production-ready enterprise Spring applications with minimal configuration.

                ---

                #### 🚀 Core Advantages
                1. **Opinionated Auto-Configuration:** Automatically configures database connections, security filters, and web servers based on classpath dependencies.
                2. **Embedded Servers:** Packages Tomcat, Jetty, or Undertow directly inside the runnable JAR.
                3. **Starter Dependencies:** Curated Maven/Gradle dependency bundles (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`).
                4. **Production-Ready Actuator:** Built-in health checks, metrics, and telemetry endpoints (`/actuator/health`).
                5. **Spring Security:** Comprehensive authentication and authorization framework.
                """
        );

        // SQL / DATABASE
        knowledgeBase.put(
            q -> q.contains("sql") || q.contains("database") || q.contains("mysql") || q.contains("postgres"),
            () -> """
                ### What is SQL?

                **SQL** (Structured Query Language) is the standard language for managing and manipulating relational databases. It enables you to create, read, update, and delete data (CRUD operations).

                ---

                #### 📊 Key Concepts

                1. **DDL (Data Definition Language):** `CREATE`, `ALTER`, `DROP` — define database schema.
                2. **DML (Data Manipulation Language):** `SELECT`, `INSERT`, `UPDATE`, `DELETE` — manipulate data.
                3. **Joins:** `INNER JOIN`, `LEFT JOIN`, `RIGHT JOIN`, `FULL JOIN` — combine rows from multiple tables.
                4. **Indexing:** B-Tree and Hash indexes for fast query performance.
                5. **Transactions:** ACID properties (Atomicity, Consistency, Isolation, Durability).

                ---

                #### 💻 Example

                ```sql
                SELECT u.name, COUNT(o.id) AS order_count
                FROM users u
                LEFT JOIN orders o ON u.id = o.user_id
                GROUP BY u.name
                HAVING COUNT(o.id) > 5
                ORDER BY order_count DESC;
                ```
                """
        );

        // HTML/CSS
        knowledgeBase.put(
            q -> q.contains("html") || q.contains("css"),
            () -> """
                ### HTML & CSS

                **HTML** (HyperText Markup Language) is the standard markup language for structuring web page content. **CSS** (Cascading Style Sheets) is used to style and layout that content.

                ---

                #### 🌐 HTML Key Concepts
                * **Semantic Elements:** `<header>`, `<nav>`, `<main>`, `<article>`, `<footer>` for meaningful structure.
                * **Forms:** `<form>`, `<input>`, `<button>` for user interaction.
                * **Accessibility:** ARIA attributes, alt text, and semantic tags for screen readers.

                #### 🎨 CSS Key Concepts
                * **Flexbox:** One-dimensional layouts (`display: flex`).
                * **Grid:** Two-dimensional layouts (`display: grid`).
                * **Responsive Design:** Media queries (`@media`), relative units (`rem`, `%`, `vw`).
                * **Animations:** `@keyframes`, `transition`, and `transform`.
                """
        );

        // GIT
        knowledgeBase.put(
            q -> q.contains("git") && (q.contains("what") || q.contains("explain") || q.contains("how") || q.contains("command")),
            () -> """
                ### What is Git?

                **Git** is a free, open-source distributed version control system designed by **Linus Torvalds** in 2005. It tracks changes in source code during software development.

                ---

                #### 📋 Essential Git Commands

                | Command | Description |
                |---|---|
                | `git init` | Initialize a new repository |
                | `git clone <url>` | Clone a remote repository |
                | `git add .` | Stage all changes |
                | `git commit -m "msg"` | Commit staged changes |
                | `git push origin main` | Push to remote |
                | `git pull` | Fetch and merge remote changes |
                | `git branch <name>` | Create a new branch |
                | `git merge <branch>` | Merge a branch into current |
                | `git log --oneline` | View commit history |
                | `git stash` | Temporarily save uncommitted changes |
                """
        );

        // API / REST
        knowledgeBase.put(
            q -> q.contains("api") || q.contains("rest api") || q.contains("restful"),
            () -> """
                ### What is a REST API?

                A **REST API** (Representational State Transfer Application Programming Interface) is an architectural style for designing networked applications. It uses standard HTTP methods to perform CRUD operations on resources.

                ---

                #### 📋 HTTP Methods

                | Method | Operation | Example |
                |---|---|---|
                | `GET` | Read | `GET /api/users` |
                | `POST` | Create | `POST /api/users` |
                | `PUT` | Update (full) | `PUT /api/users/1` |
                | `PATCH` | Update (partial) | `PATCH /api/users/1` |
                | `DELETE` | Delete | `DELETE /api/users/1` |

                #### 🔑 REST Principles
                1. **Stateless:** Each request contains all information needed (no server-side session).
                2. **Resource-Based:** Everything is a resource identified by a URI.
                3. **HTTP Status Codes:** 200 (OK), 201 (Created), 400 (Bad Request), 401 (Unauthorized), 404 (Not Found), 500 (Internal Server Error).
                """
        );

        // DOCKER / KUBERNETES
        knowledgeBase.put(
            q -> q.contains("docker") || q.contains("kubernetes") || q.contains("k8s") || q.contains("container"),
            () -> """
                ### Docker & Kubernetes

                **Docker** is a platform for building, shipping, and running applications in lightweight, isolated containers. **Kubernetes (K8s)** is an open-source container orchestration system.

                ---

                #### 🐳 Docker Key Concepts
                * **Image:** A read-only template containing application code and dependencies.
                * **Container:** A running instance of an image — lightweight, portable, and isolated.
                * **Dockerfile:** Script defining how to build an image (`FROM`, `COPY`, `RUN`, `CMD`).
                * **Docker Compose:** Tool for defining multi-container applications using `docker-compose.yml`.

                #### ☸️ Kubernetes Key Concepts
                * **Pod:** Smallest deployable unit, contains one or more containers.
                * **Service:** Stable network endpoint for accessing pods.
                * **Deployment:** Manages replicas and rolling updates.
                * **Ingress:** Routes external HTTP traffic to internal services.
                """
        );

        // === GENERAL KNOWLEDGE ===

        // INDIA INDEPENDENCE
        knowledgeBase.put(
            q -> q.contains("india") && (q.contains("independence") || q.contains("independent") || q.contains("freedom")),
            () -> """
                ### India's Independence

                **India gained independence on August 15, 1947**, ending nearly 200 years of British colonial rule.

                ---

                #### 📜 Key Facts

                1. **Date:** August 15, 1947
                2. **Colonial Power:** British Empire (East India Company rule from 1757; British Crown rule from 1858)
                3. **Key Leaders:**
                   * **Mahatma Gandhi** — Led non-violent civil disobedience movements (Salt March, Quit India)
                   * **Jawaharlal Nehru** — First Prime Minister of independent India
                   * **Sardar Vallabhbhai Patel** — Unified 562 princely states into the Indian Union
                   * **Subhas Chandra Bose** — Led the Indian National Army (INA)
                   * **B.R. Ambedkar** — Chief architect of the Indian Constitution
                4. **Indian Independence Act 1947:** Passed by the British Parliament, partitioning British India into **India** and **Pakistan**.
                5. **First Prime Minister:** Jawaharlal Nehru delivered the famous **"Tryst with Destiny"** speech at midnight on August 14–15, 1947.
                6. **Republic Day:** India adopted its Constitution on **January 26, 1950**, becoming a sovereign democratic republic.
                """
        );

        // PRIME MINISTER OF INDIA
        knowledgeBase.put(
            q -> q.contains("prime minister") && q.contains("india"),
            () -> """
                ### Prime Minister of India

                As of 2024, the **Prime Minister of India** is **Narendra Damodardas Modi**.

                ---

                #### 📋 Key Details

                | Detail | Information |
                |---|---|
                | **Full Name** | Narendra Damodardas Modi |
                | **Born** | September 17, 1950, Vadnagar, Gujarat |
                | **Party** | Bharatiya Janata Party (BJP) |
                | **First Term** | May 26, 2014 |
                | **Second Term** | May 30, 2019 |
                | **Third Term** | June 9, 2024 |

                #### 🇮🇳 Notable Previous Prime Ministers
                1. **Jawaharlal Nehru** (1947–1964) — First PM, architect of modern India
                2. **Lal Bahadur Shastri** (1964–1966) — "Jai Jawan Jai Kisan"
                3. **Indira Gandhi** (1966–1977, 1980–1984) — First female PM
                4. **Atal Bihari Vajpayee** (1998–2004) — Led nuclear tests (Pokhran-II)
                5. **Manmohan Singh** (2004–2014) — Economic liberalization architect
                """
        );

        // PLANTS
        knowledgeBase.put(
            q -> q.contains("plant") && (q.contains("name") || q.contains("list") || q.contains("tell") || q.contains("5") || q.contains("ten") || q.contains("example")),
            () -> """
                ### 🌿 Common Plants

                Here are some well-known plants from around the world:

                ---

                | # | Plant Name | Scientific Name | Key Feature |
                |---|---|---|---|
                | 1 | **Rose** | *Rosa* | Fragrant flowering plant, symbol of love |
                | 2 | **Tulsi (Holy Basil)** | *Ocimum tenuiflorum* | Sacred medicinal herb in Indian culture |
                | 3 | **Sunflower** | *Helianthus annuus* | Heliotropic flower that tracks the sun |
                | 4 | **Neem** | *Azadirachta indica* | Medicinal tree with antibacterial properties |
                | 5 | **Aloe Vera** | *Aloe barbadensis* | Succulent with healing gel for skin care |
                | 6 | **Bamboo** | *Bambusoideae* | Fastest-growing plant (up to 91 cm/day) |
                | 7 | **Mango Tree** | *Mangifera indica* | National fruit tree of India |
                | 8 | **Lavender** | *Lavandula* | Aromatic herb used in perfumes and therapy |
                | 9 | **Banyan Tree** | *Ficus benghalensis* | National tree of India, known for aerial roots |
                | 10 | **Lotus** | *Nelumbo nucifera* | National flower of India, aquatic plant |
                """
        );

        // ANIMALS
        knowledgeBase.put(
            q -> q.contains("animal") && (q.contains("name") || q.contains("list") || q.contains("tell") || q.contains("example")),
            () -> """
                ### 🐾 Common Animals

                | # | Animal | Type | Habitat | Fun Fact |
                |---|---|---|---|---|
                | 1 | **Lion** | Mammal | African savannas | Called the "King of the Jungle" |
                | 2 | **Elephant** | Mammal | Africa & Asia | Largest land animal |
                | 3 | **Peacock** | Bird | South Asia | National bird of India |
                | 4 | **Dolphin** | Marine Mammal | Oceans worldwide | Highly intelligent, uses echolocation |
                | 5 | **Tiger** | Mammal | Asia | National animal of India |
                | 6 | **Eagle** | Bird | Worldwide | Exceptional eyesight (8x human vision) |
                | 7 | **Whale** | Marine Mammal | All oceans | Blue whale is the largest animal ever |
                | 8 | **Penguin** | Bird | Antarctica | Flightless, excellent swimmers |
                """
        );

        // COUNTRIES / CAPITALS
        knowledgeBase.put(
            q -> q.contains("capital") || (q.contains("countr") && (q.contains("name") || q.contains("list"))),
            () -> """
                ### 🌍 Countries and Their Capitals

                | # | Country | Capital | Continent |
                |---|---|---|---|
                | 1 | **India** | New Delhi | Asia |
                | 2 | **United States** | Washington, D.C. | North America |
                | 3 | **United Kingdom** | London | Europe |
                | 4 | **Japan** | Tokyo | Asia |
                | 5 | **Australia** | Canberra | Oceania |
                | 6 | **France** | Paris | Europe |
                | 7 | **Germany** | Berlin | Europe |
                | 8 | **Brazil** | Brasília | South America |
                | 9 | **Canada** | Ottawa | North America |
                | 10 | **China** | Beijing | Asia |
                """
        );

        // SOLAR SYSTEM / PLANETS
        knowledgeBase.put(
            q -> q.contains("planet") || q.contains("solar system") || q.contains("mercury") || q.contains("mars") || q.contains("jupiter"),
            () -> """
                ### 🌌 The Solar System

                Our Solar System consists of the **Sun**, 8 planets, dwarf planets, moons, asteroids, and comets.

                ---

                #### 🪐 The 8 Planets (in order from the Sun)

                | # | Planet | Type | Key Fact |
                |---|---|---|---|
                | 1 | **Mercury** | Rocky | Closest to the Sun, no atmosphere |
                | 2 | **Venus** | Rocky | Hottest planet (462°C), rotates backward |
                | 3 | **Earth** | Rocky | Only known planet with life |
                | 4 | **Mars** | Rocky | "Red Planet", has the tallest volcano (Olympus Mons) |
                | 5 | **Jupiter** | Gas Giant | Largest planet, Great Red Spot storm |
                | 6 | **Saturn** | Gas Giant | Famous for its ring system |
                | 7 | **Uranus** | Ice Giant | Rotates on its side |
                | 8 | **Neptune** | Ice Giant | Strongest winds in the solar system |

                > **Note:** Pluto was reclassified as a **dwarf planet** in 2006 by the International Astronomical Union (IAU).
                """
        );

        // MATHEMATICS
        knowledgeBase.put(
            q -> q.contains("math") || q.contains("algebra") || q.contains("calculus") || q.contains("geometry") || q.contains("trigonometry"),
            () -> """
                ### 📐 Mathematics Overview

                **Mathematics** is the study of numbers, quantities, shapes, and patterns. It is the foundation of science, engineering, technology, and finance.

                ---

                #### 📊 Major Branches

                1. **Arithmetic:** Basic operations — addition, subtraction, multiplication, division.
                2. **Algebra:** Variables, equations, polynomials (`ax² + bx + c = 0`).
                3. **Geometry:** Shapes, areas, volumes, angles (Euclidean and non-Euclidean).
                4. **Trigonometry:** Relationships between angles and sides of triangles (`sin`, `cos`, `tan`).
                5. **Calculus:** Limits, derivatives (rate of change), integrals (area under curve).
                6. **Statistics & Probability:** Data analysis, mean, median, mode, distributions.
                7. **Linear Algebra:** Vectors, matrices, eigenvalues, used extensively in AI/ML.

                ---

                #### 💡 Famous Formulas
                * **Pythagorean Theorem:** `a² + b² = c²`
                * **Quadratic Formula:** `x = (-b ± √(b²-4ac)) / 2a`
                * **Euler's Identity:** `e^(iπ) + 1 = 0` (considered the most beautiful equation)
                """
        );

        // SCIENCE / PHYSICS
        knowledgeBase.put(
            q -> q.contains("physics") || q.contains("science") || q.contains("newton") || q.contains("gravity") || q.contains("atom"),
            () -> """
                ### 🔬 Science & Physics

                **Physics** is the natural science that studies matter, energy, motion, and the fundamental forces of the universe.

                ---

                #### ⚛️ Key Branches

                1. **Classical Mechanics:** Newton's Laws of Motion, gravitation, momentum.
                2. **Thermodynamics:** Heat, entropy, energy transfer, laws of thermodynamics.
                3. **Electromagnetism:** Electric fields, magnetic fields, Maxwell's equations, light.
                4. **Quantum Mechanics:** Wave-particle duality, Heisenberg uncertainty, Schrödinger equation.
                5. **Relativity:** Einstein's Special and General theories (E = mc²).

                ---

                #### 📜 Newton's Three Laws of Motion
                1. **First Law (Inertia):** An object at rest stays at rest unless acted upon by a force.
                2. **Second Law:** Force = Mass × Acceleration (`F = ma`).
                3. **Third Law:** For every action, there is an equal and opposite reaction.
                """
        );

        // HISTORY (GENERAL)
        knowledgeBase.put(
            q -> q.contains("history") || q.contains("world war") || q.contains("ancient") || q.contains("civilization"),
            () -> """
                ### 📜 World History Overview

                ---

                #### 🏛️ Major Historical Periods

                1. **Ancient Civilizations (3000 BCE – 500 CE):**
                   * Mesopotamia, Ancient Egypt, Indus Valley, Ancient Greece, Roman Empire.

                2. **Medieval Period (500 – 1500 CE):**
                   * Byzantine Empire, Islamic Golden Age, European Feudalism, Crusades.

                3. **Renaissance & Age of Exploration (1400 – 1600):**
                   * Revival of art and science in Europe, Columbus reaches Americas (1492).

                4. **Industrial Revolution (1760 – 1840):**
                   * Shift from agrarian to industrial economies, steam engine, factories.

                5. **World War I (1914 – 1918):**
                   * Allied Powers vs Central Powers, trench warfare, ~17 million deaths.

                6. **World War II (1939 – 1945):**
                   * Allied vs Axis powers, Holocaust, atomic bombs on Hiroshima & Nagasaki, ~70–85 million deaths.

                7. **Modern Era (1945 – Present):**
                   * Cold War, Space Race, Internet revolution, globalization.
                """
        );

        // GEOGRAPHY
        knowledgeBase.put(
            q -> q.contains("geography") || q.contains("continent") || q.contains("ocean") || q.contains("mountain") || q.contains("river"),
            () -> """
                ### 🌍 Geography

                ---

                #### 🗺️ The 7 Continents
                1. **Asia** — Largest continent (44.6 million km²)
                2. **Africa** — Second largest, 54 countries
                3. **North America** — Includes USA, Canada, Mexico
                4. **South America** — Amazon Rainforest, Andes Mountains
                5. **Antarctica** — Coldest, driest, covered in ice
                6. **Europe** — 44 countries, rich cultural history
                7. **Australia/Oceania** — Smallest continent

                #### 🌊 The 5 Oceans
                1. **Pacific Ocean** — Largest (165.25 million km²)
                2. **Atlantic Ocean** — Second largest
                3. **Indian Ocean** — Warmest ocean
                4. **Southern Ocean** — Surrounds Antarctica
                5. **Arctic Ocean** — Smallest, mostly frozen

                #### 🏔️ Notable Features
                * **Highest Peak:** Mount Everest (8,849 m), Nepal/Tibet
                * **Longest River:** Nile (6,650 km), Africa
                * **Largest Desert:** Sahara (9.2 million km²), Africa
                """
        );

        // AI / MACHINE LEARNING
        knowledgeBase.put(
            q -> q.contains("artificial intelligence") || q.contains(" ai ") || q.contains("machine learning") || q.contains("deep learning") || q.contains("neural network"),
            () -> """
                ### 🤖 Artificial Intelligence & Machine Learning

                **Artificial Intelligence (AI)** is the simulation of human intelligence by machines. **Machine Learning (ML)** is a subset of AI where systems learn from data without being explicitly programmed.

                ---

                #### 🧠 Key Concepts

                1. **Supervised Learning:** Training on labeled data (classification, regression).
                2. **Unsupervised Learning:** Finding patterns in unlabeled data (clustering, dimensionality reduction).
                3. **Deep Learning:** Neural networks with multiple layers (CNNs for images, RNNs/Transformers for text).
                4. **Natural Language Processing (NLP):** Understanding and generating human language (ChatGPT, BERT, GPT-4).
                5. **Computer Vision:** Image recognition, object detection, facial recognition.

                #### 🛠️ Popular Frameworks
                * **PyTorch** — Research-friendly, dynamic computation graph
                * **TensorFlow** — Production-grade, static graphs, TensorFlow Lite for mobile
                * **Scikit-learn** — Classical ML algorithms
                * **HuggingFace Transformers** — Pre-trained NLP models
                """
        );

        // CLOUD COMPUTING
        knowledgeBase.put(
            q -> q.contains("cloud") || q.contains("aws") || q.contains("azure") || q.contains("gcp"),
            () -> """
                ### ☁️ Cloud Computing

                **Cloud Computing** is the delivery of computing services (servers, storage, databases, networking, software) over the internet ("the cloud") on a pay-as-you-go basis.

                ---

                #### 📊 Service Models

                | Model | Description | Examples |
                |---|---|---|
                | **IaaS** | Infrastructure as a Service | AWS EC2, Azure VMs, Google Compute Engine |
                | **PaaS** | Platform as a Service | Heroku, Google App Engine, AWS Elastic Beanstalk |
                | **SaaS** | Software as a Service | Gmail, Salesforce, Microsoft 365 |

                #### 🏢 Major Cloud Providers
                1. **Amazon Web Services (AWS)** — Market leader, 200+ services
                2. **Microsoft Azure** — Strong enterprise integration
                3. **Google Cloud Platform (GCP)** — AI/ML leadership, BigQuery
                """
        );

        // === INTELLIGENT FALLBACK ===
        // Try the ordered knowledge base
        for (var entry : knowledgeBase.entrySet()) {
            if (entry.getKey().test(p)) {
                return entry.getValue().get();
            }
        }

        // === SMART GENERAL FALLBACK ===
        return generateIntelligentFallback(prompt, p);
    }

    private String generateIntelligentFallback(String originalPrompt, String lowerPrompt) {
        // Detect question type and generate a contextually appropriate response
        String topic = extractTopic(originalPrompt);

        if (lowerPrompt.contains("what is") || lowerPrompt.contains("what are") || lowerPrompt.contains("explain") || lowerPrompt.contains("define")) {
            return String.format("""
                ### %s

                Based on your question, here is a comprehensive overview:

                ---

                **%s** is a topic that encompasses several important concepts and principles. While I can provide a structured overview, for the most detailed and up-to-date information, I recommend consulting specialized resources or enabling a live AI provider (Google Gemini or OpenAI) in your Nexus AI configuration.

                ---

                #### 📌 What You Should Know
                * This topic relates to widely studied and documented concepts across academic and professional domains.
                * Understanding the fundamentals will help build a strong foundation for practical applications.

                #### 💡 How to Get More Detailed Answers
                To unlock full AI-powered responses with real-time knowledge:
                1. Add a valid **Google Gemini API key** to your `.env` file: `GEMINI_API_KEY=your_key_here`
                2. Or add an **OpenAI API key**: `OPENAI_API_KEY=your_key_here`
                3. Restart the backend server

                This will connect Nexus AI to a live language model for comprehensive, real-time answers to any question.

                ---
                *Ask me about programming (Java, Python, React, SQL), science, history, geography, mathematics, or technology for detailed built-in answers!*
                """, topic, topic);
        }

        if (lowerPrompt.contains("how to") || lowerPrompt.contains("how do") || lowerPrompt.contains("how can")) {
            return String.format("""
                ### How To: %s

                Here is a structured approach to your question:

                ---

                #### 📋 Step-by-Step Guide

                1. **Understand the Goal:** Clearly define what you want to achieve with "%s".
                2. **Research & Plan:** Gather information about best practices and common approaches.
                3. **Implement:** Start with a basic implementation and iterate.
                4. **Test & Validate:** Verify your solution works as expected.
                5. **Optimize:** Refine and improve based on results.

                ---

                #### 💡 For More Detailed Instructions
                To get step-by-step tutorials with code examples and real-time guidance, configure a live AI provider in your `.env` file:
                * `GEMINI_API_KEY=your_google_gemini_key`
                * `OPENAI_API_KEY=your_openai_key`

                ---
                *Ask me about programming, science, history, geography, or technology for detailed built-in answers!*
                """, topic, topic);
        }

        if (lowerPrompt.contains("who is") || lowerPrompt.contains("who was") || lowerPrompt.contains("who are")) {
            return String.format("""
                ### About: %s

                Your question asks about a person or group of people. Here is what I can share:

                ---

                #### 📌 Overview
                * **Topic:** %s
                * For detailed biographical information, historical context, and current facts about individuals, I recommend enabling a live AI provider.

                #### 💡 How to Get Complete Biographical Answers
                Configure a live AI provider in your `.env` file for real-time knowledge:
                * `GEMINI_API_KEY=your_google_gemini_key`
                * `OPENAI_API_KEY=your_openai_key`

                ---
                *I have detailed built-in knowledge about India's Prime Ministers, independence history, programming languages, science, geography, and more. Try asking about those topics!*
                """, topic, originalPrompt);
        }

        if (lowerPrompt.contains("list") || lowerPrompt.contains("name") || lowerPrompt.contains("tell") || lowerPrompt.contains("give me") || lowerPrompt.matches(".*\\d+.*")) {
            return String.format("""
                ### %s

                You've asked for a list or set of items. Here's what I can help with:

                ---

                #### 📌 Available Built-in Lists
                I have detailed knowledge about:
                * 🌿 **Plants** — "tell me 5 plant names"
                * 🐾 **Animals** — "list common animals"
                * 🌍 **Countries & Capitals** — "list countries and capitals"
                * 🪐 **Planets** — "name the planets in the solar system"
                * 💻 **Programming Languages** — "list programming languages"

                #### 💡 For Custom Lists
                To get any custom list generated in real-time, configure a live AI provider:
                * `GEMINI_API_KEY=your_google_gemini_key` in your `.env` file

                ---
                *Try rephrasing your question to match one of the topics above, or enable a live AI provider for unlimited answers!*
                """, topic);
        }

        // Ultimate fallback
        return String.format("""
                ### Nexus AI Response

                Thank you for your question: **"%s"**

                ---

                #### 📌 About This Response
                I currently have detailed built-in knowledge for the following topics:

                | Category | Topics |
                |---|---|
                | **Programming** | Java, Python, JavaScript, React, Spring Boot, SQL, HTML/CSS, Git, REST APIs, Docker, Kubernetes |
                | **Computer Science** | OOP, Data Structures, AI/ML, Cloud Computing |
                | **General Knowledge** | Indian History, Prime Ministers, Geography, Solar System, Plants, Animals |
                | **Science & Math** | Physics, Mathematics, Algebra, Calculus |

                #### 💡 How to Unlock Unlimited Knowledge
                To get ChatGPT/Gemini-quality answers for **any** question:

                1. Get a free API key from [Google AI Studio](https://aistudio.google.com/apikey)
                2. Add it to your `.env` file: `GEMINI_API_KEY=your_key_here`
                3. Restart the backend server (`./mvnw spring-boot:run`)

                This connects Nexus AI to a live language model capable of answering any question with real-time, comprehensive knowledge.

                ---
                *Try asking me about Java, Python, React, OOP, India, planets, plants, or animals for detailed answers right now!*
                """, originalPrompt != null ? originalPrompt : "General Inquiry");
    }

    private String extractTopic(String prompt) {
        if (prompt == null || prompt.isBlank()) return "General Inquiry";
        String cleaned = prompt.trim();
        // Remove common question prefixes
        String[] prefixes = {"what is ", "what are ", "who is ", "who was ", "explain ", "define ", "how to ", "how do ", "how can ", "tell me about ", "describe "};
        for (String prefix : prefixes) {
            if (cleaned.toLowerCase().startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length()).trim();
                break;
            }
        }
        // Capitalize first letter
        if (!cleaned.isEmpty()) {
            cleaned = cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
        }
        // Remove trailing question mark
        if (cleaned.endsWith("?")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }
}

# Nexus AI — Consumer AI SaaS Subscription & Usage Platform (v1.0)

A production-grade, full-stack **Consumer AI SaaS Platform** built with **Spring Boot 3 (Java 17)**, **PostgreSQL 16**, **Spring Security + JWT with Refresh Token Rotation**, and **React 19 (Vite + Tailwind CSS + Lucide Icons)**.

---

## 🌟 Architecture & Product Topology

Nexus AI is built on a direct-ownership **Consumer B2C architecture**. Every customer owns their personal account, active subscription, atomic usage quotas, payment orders, tax invoices, and multi-tool AI Studio sessions.

```text
                             NEXUS AI PLATFORM
                                     │
                     ┌───────────────┴───────────────┐
                     │                               │
                   USER                            ADMIN
                     │                               │
        ┌────────────┼────────────┐     ┌────────────┼────────────┐
        │            │            │     │            │            │
    AI Studio      Usage    Subscription Users     Plans      Analytics
  (Text/Image/       │            │     │            │            │
  Search/Research/   └─────┬──────┘     └─────┬──────┘            │
  Multimodal/Voice)        │                  │                   │
                        Payments           Payments          Audit / Health
                           │                  │
                        Invoices           Invoices
```

### Direct Ownership Model
```text
User (ROLE_USER / ROLE_ADMIN)
 ├── Active Subscription (user_id FK) ──► Plan (FREE, PRO, BUSINESS)
 ├── Usage Records (user_id FK, Atomic Pessimistic Quota Locking)
 ├── Payment Orders (user_id FK, Razorpay Gateway + Sandbox Fallback)
 ├── Immutable Tax Invoices (user_id FK, GST 18%, Sequential Reference)
 ├── Notifications & Threshold Alerts (75%, 90%, 100% Idempotent Delivery)
 ├── Security Audit Logs (Auth, Billing, Inference, Lifecycle Events)
 └── Refresh Tokens (SHA-256 Hashed, Multi-Session Revocation)
```

---

## 🚀 Complete Platform Capabilities (v1.0)

### 1. AI Studio Multi-Tool Suite (`/studio`)
* **Standard AI Generation & Multi-turn Chat**: High-throughput contextual generation powered by Google Gemini and OpenAI models.
* **AI Image Generation (Phase 5B)**: Text-to-image synthesis with aspect ratio selection (`1:1`, `16:9`, `9:16`, `4:3`), style presets (`Cinematic`, `Photorealistic`, `Anime`, `Cyberpunk`), lightbox zoom, and instant download.
* **Multimodal Document & Image Understanding (Phase 5C)**: In-memory parsing and visual reasoning for images, PDFs, code files (`.js`, `.py`, `.java`, `.sql`), JSON, CSV, and markdown with a 10MB safety guard.
* **Real-time Web Search & Citations (Phase 5C)**: Live web crawling via **Tavily Search API**, normalizing sources, synthesizing grounded responses with interactive `[S1]`, `[S2]` citation badges, and a dedicated **Sources Card** grid.
* **Multi-query Deep Research Engine (Phase 5D)**: Multi-angle query planner, URL deduplication, domain authority evaluation (`.gov`, `.edu`, arXiv, Nature, Reuters), grounded LLM synthesis, and structured report cards (Executive Summary, Key Findings, Technical Breakdown, Opposing Evidence, Limitations, Conclusion).
* **Live Voice Input & Speech-to-Text (Phase 5E)**: Browser Web Speech API transcription supporting English (India), English (US), Hindi, and Kannada with live waveform status and typed text preservation. Zero audio is stored or uploaded to server.

### 2. Transactional Registration & Free Tier Quota
* Seamless sign-up automatically provisions a `User` account, active `FREE` Plan subscription with 50 requests/month, and an onboarding welcome notification in a **single database transaction**.

### 3. Authoritative Billing & Invoicing (`/subscription`, `/invoices`)
* Server computes plan prices and 18% GST server-side (preventing client-side tampering).
* Razorpay payment order creation and signature verification with automatic sandbox fallback.
* Webhook ingestion with SHA-256 idempotency deduplication (`/api/v1/billing/webhook`).
* Immutable tax invoices with sequential reference numbers (`INV-YYYYMM-XXXXX`).

### 4. Atomic Quota Metering & Threshold Alerts (`/notifications`)
* Atomic pessimistic write locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) guarantees zero over-consumption under concurrent load.
* Automated idempotent threshold warning notifications dispatched at 75%, 90%, and 100% quota consumption.

### 5. User Security & Session Management (`/settings`)
* BCrypt password hashing with high work factor.
* Refresh token rotation with replay detection.
* Changing password automatically invalidates and revokes all active sessions.

### 6. Platform Operator Admin Console (`/admin/**`)
* Strictly guarded by `hasRole('ADMIN')` (unauthorized users receive HTTP 403 Forbidden).
* Real-time financial telemetry calculated from PostgreSQL: **MRR**, **ARR**, **Total Revenue**, **Conversion Rate**, **Churn Rate**, and **ARPPU**.
* Full user dossier inspection, status controls (`ACTIVE`, `SUSPENDED`, `DEACTIVATED`), dynamic plan pricing updates, master audit log browser, and live JVM heap health monitoring.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 17, Spring Boot 3.2.3, Spring Security 6, Spring Data JPA / Hibernate, PostgreSQL, H2 (Test Profile), JJWT 0.12.5, Lombok, Jakarta Validation |
| **Frontend** | React 19, Vite, Tailwind CSS, Lucide React, Axios, Web Speech API |
| **AI Providers** | Google Gemini (2.0 Flash / 1.5 Flash / 1.5 Pro), OpenAI (GPT-4o / DALL-E), Pollinations AI (FLUX / SD), Mock AI Provider |
| **Search Engine** | Tavily Search API, Mock Search Engine |
| **Payment Gateway** | Razorpay / Sandbox Provider |
| **Infrastructure** | PostgreSQL 16, Docker Compose, Maven |

---

## 🏁 Quickstart & Local Setup

### 1. Start PostgreSQL (Docker)
```bash
docker compose up -d
```
*Database: `saas_platform`, User: `saas_user`, Password: `saas_password` on port `5432`.*

### 2. Configure Environment Variables
Copy `.env.example` to `.env` in the project root:
```bash
cp .env.example .env
```
Populate your API keys:
* `GEMINI_API_KEY`: Get a free key at [Google AI Studio](https://aistudio.google.com/app/apikey)
* `TAVILY_API_KEY`: Get a free key at [Tavily](https://tavily.com)
* `OPENAI_API_KEY`: *(Optional)* OpenAI key for GPT-4o / DALL-E

### 3. Start Backend
```bash
cd backend
.\mvnw.cmd spring-boot:run
```
API server runs on `http://localhost:8080`. Health endpoint: `http://localhost:8080/actuator/health`.

### 4. Start Frontend
```bash
cd frontend
npm install
npm run dev
```
Application accessible at `http://localhost:5173`.

---

## 🧪 Automated Testing & Production Build

### Run Backend Test Suite (82 Tests)
```bash
cd backend
.\mvnw.cmd clean test
```
*Executes all 82 unit, integration, security, concurrency, image, multimodal, web search, and deep research tests.*

### Build Frontend Production Bundle
```bash
cd frontend
npm run build
```
*Compiles the optimized production bundle to `frontend/dist` with zero errors.*

---

## 📖 Key API Endpoints Reference

### AI Studio Endpoints
* `POST /api/v1/ai/generate` — Text generation with atomic quota deduction
* `POST /api/v1/ai/chat` — Conversational multi-turn chat
* `POST /api/v1/ai/image/generate` — Text-to-image synthesis
* `POST /api/v1/ai/multimodal` — In-memory multimodal file & document inference
* `POST /api/v1/ai/search` — Query web search returning normalized sources
* `POST /api/v1/ai/search/generate` — Grounded web search + AI answer synthesis with citations
* `POST /api/v1/ai/research` — Multi-query Deep Research engine with structured report
* `GET /api/v1/ai/models` — List available LLM models
* `GET /api/v1/ai/image/models` — List available image synthesis models

### Authentication & Account
* `POST /api/v1/auth/register` — User registration with instant FREE plan provisioning
* `POST /api/v1/auth/login` — Sign in and receive access/refresh JWT tokens
* `POST /api/v1/auth/refresh` — Refresh token rotation
* `POST /api/v1/auth/logout` — Revoke active refresh token
* `GET /api/v1/users/me/sessions` — Active user session list
* `POST /api/v1/users/me/change-password` — Change password & revoke all active sessions

### Billing & Subscription
* `GET /api/v1/usage/current` — Current billing cycle usage metrics and limits
* `GET /api/v1/billing/subscription/current` — Active subscription status
* `POST /api/v1/billing/orders/create` — Create server-calculated payment order
* `POST /api/v1/billing/orders/verify` — Verify gateway signature and upgrade tier
* `GET /api/v1/billing/invoices` — List customer tax invoices
* `GET /api/v1/billing/invoices/{id}` — View single invoice (isolated to owner)

### Admin Operations (`ROLE_ADMIN`)
* `GET /api/v1/admin/dashboard` — Platform overview & operational KPIs
* `GET /api/v1/admin/analytics` — Authoritative financial metrics (MRR, ARR, Churn, ARPPU)
* `GET /api/v1/admin/users` — Paginated & searchable customer directory
* `PATCH /api/v1/admin/users/{id}/status` — Modify user status (`ACTIVE`, `SUSPENDED`, `DEACTIVATED`)
* `GET /api/v1/admin/plans` — List and configure subscription tier limits
* `GET /api/v1/admin/audit-logs` — Master platform security audit trail
* `GET /api/v1/admin/health` — Live JVM heap telemetry and system status

---

## 🔒 Security & Privacy Posture
* **Zero Audio Upload**: Voice input is recognized client-side via Web Speech API; no audio files are stored or sent to backend.
* **Strict IDOR Isolation**: All customer resources are queried with `UserPrincipal.getId()`.
* **Zero Secret Leakage**: All API keys (`GEMINI_API_KEY`, `TAVILY_API_KEY`, `JWT_SECRET`, etc.) are processed exclusively on the backend and protected by `.gitignore`.
* **No Raw Chain-of-Thought Exposure**: Only high-level user-facing execution milestones are presented.

---

## 📄 License
MIT License. Built for production-ready consumer SaaS deployment.

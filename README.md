# Nexus AI — Consumer AI SaaS Subscription & Usage Platform

A production-grade **Consumer AI SaaS Platform** (modeled after applications like ChatGPT/Gemini) built with **Spring Boot 3 (Java 17)**, **PostgreSQL**, **Spring Security + JWT with Refresh Token Rotation**, and **React (Vite + Tailwind CSS + Lucide Icons)**.

---

## 🌟 Architecture & Product Topology

This is a **direct-ownership Consumer AI SaaS Platform**. Normal customers directly own their subscriptions, quota, payments, invoices, and AI inference sessions.

```text
                    AI SaaS PLATFORM
                           │
             ┌─────────────┴─────────────┐
             │                           │
           USER                        ADMIN
             │                           │
      ┌──────┼──────┐             ┌──────┼──────┐
      │      │      │             │      │      │
     AI   Usage  Subscription    Users  Plans  Analytics
             │      │             │      │      │
             └──────┼─────────────┴──────┘      │
                    │                           │
                 Payments                    Health / Audit
                    │
                 Invoices
```

### Direct Ownership Hierarchy
```text
User (USER / ADMIN)
 ├── Active Subscription (user_id FK) ──► Plan (FREE, PRO, BUSINESS)
 ├── Usage Records (user_id FK, Pessimistic Locking Quota Checks)
 ├── Payment Orders (user_id FK, Razorpay / Sandbox Gateway)
 ├── Immutable Tax Invoices (user_id FK, GST 18%, Sequential ID)
 ├── Notifications & Quota Alerts (75%, 90%, 100% Thresholds)
 ├── Security Audit Logs (Auth, Billing, Lifecycle events)
 └── Refresh Tokens (SHA-256 Hashed, Multi-Session Revocation)
```

---

## 🚀 Key Platform Features

1. **AI Studio & Model Inference (`/studio`)**:
   - Interactive prompt engineering and conversational chat workbench.
   - Multi-model provider abstraction (`Gemini 1.5 Flash`, `Gemini 1.5 Pro`, `GPT-4o`, `Claude 3.5 Sonnet`).
   - Atomic pre-inference quota check with pessimistic locking (`PESSIMISTIC_WRITE`). Returns **HTTP 429 Too Many Requests** when quota is exhausted.

2. **Transactional Registration & Instant FREE Quota**:
   - User registration atomically provisions `User` + `FREE` Plan `Subscription` (50 AI requests/month) + Welcome Notification in **1 single database transaction**.

3. **Authoritative Server-Side Billing & Invoicing (`/subscription`, `/invoices`)**:
   - Server computes base price + 18% GST (never trusting client amounts).
   - Razorpay gateway integration with automatic Sandbox fallback.
   - Webhook processing with SHA-256 idempotency deduplication (`/api/v1/billing/webhook`).
   - Immutable PDF/Print-ready tax invoices (`INV-YYYYMM-XXXXX`).

4. **Idempotent Quota Threshold Alerts & Notifications (`/notifications`)**:
   - Automated warnings at 75%, 90%, and 100% quota consumption.
   - Guaranteed idempotency per billing cycle.
   - Real-time unread counts and mark-read actions.

5. **User Settings & Session Invalidation (`/settings`)**:
   - Profile updates and password changes.
   - Changing password automatically terminates and revokes all active refresh tokens in database.

6. **Platform Operator Console (`/admin/**`)**:
   - Restricted to `ROLE_ADMIN` (`hasRole('ADMIN')`). Returns HTTP 403 Forbidden to normal `USER` accounts.
   - Real-time **MRR**, **ARR**, **Total Revenue**, **Conversion Rate**, **Churn Rate**, and **ARPPU** calculated directly from PostgreSQL.
   - User status controls (`ACTIVE`, `SUSPENDED`, `DEACTIVATED`).
   - Live plan pricing and quota configuration.
   - Security audit logs inspector and live JVM memory/uptime telemetry.

7. **Rate Limiting & Security Hardening**:
   - Sliding-window token-bucket rate limiter guarding high-risk endpoints (`/auth/login`, `/auth/register`, `/auth/refresh`, `/users/me/change-password`, `/billing/orders/**`, `/billing/webhook`).
   - Security headers: `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`.

---

## 🛠️ Technology Stack

* **Backend**: Java 17, Spring Boot 3.2.3, Spring Security 6, Spring Data JPA / Hibernate, PostgreSQL, H2 (Test Profile), JJWT 0.12.5, Lombok, Jakarta Validation.
* **Frontend**: React 19, Vite, Tailwind CSS 4, React Router v7, Axios, Lucide React.
* **Infrastructure**: PostgreSQL 16, Docker Compose, Maven.

---

## 🏁 Getting Started

### 1. Database Setup (Docker Compose)
```bash
docker compose up -d
```
*Database name: `saas_platform`, User: `saas_user`, Password: `saas_password` on port `5432`.*

### 2. Running the Backend
```bash
cd backend
mvn spring-boot:run
```
Backend API will start at: `http://localhost:8080`

### 3. Running the Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend will be accessible at: `http://localhost:5173`

---

## 🧪 Automated Test Verification

Run all 45 automated unit & integration tests:
```bash
cd backend
mvn clean test
```

Build the frontend production bundle:
```bash
cd frontend
npm run build
```

---

## 📖 API Documentation Reference

### Consumer Endpoints
* `POST /api/v1/auth/register` — Register User & provision FREE plan
* `POST /api/v1/auth/login` — Sign in and retrieve JWT tokens
* `POST /api/v1/auth/refresh` — Rotate refresh token
* `POST /api/v1/auth/logout` — Revoke refresh token
* `POST /api/v1/ai/generate` — Generate text with quota check
* `POST /api/v1/ai/chat` — Conversational AI turn with quota check
* `GET /api/v1/ai/models` — List available AI models
* `GET /api/v1/usage/current` — Current billing cycle usage & quotas
* `GET /api/v1/billing/subscription/current` — Active subscription details
* `POST /api/v1/billing/orders/create` — Create server-calculated payment order
* `POST /api/v1/billing/orders/verify` — Verify gateway signature and upgrade tier
* `GET /api/v1/billing/invoices` — List customer tax invoices
* `GET /api/v1/billing/invoices/{id}` — Get invoice detail (isolated to owner)
* `GET /api/v1/notifications` — Get user notifications
* `PATCH /api/v1/users/me` — Update user profile
* `POST /api/v1/users/me/change-password` — Change password & revoke all sessions

### Admin Operator Endpoints (Requires `ROLE_ADMIN`)
* `GET /api/v1/admin/dashboard` — Platform overview & KPI telemetry
* `GET /api/v1/admin/analytics` — Authoritative MRR, ARR, conversion, churn, ARPPU
* `GET /api/v1/admin/users` — Paginated & searchable customer directory
* `GET /api/v1/admin/users/{id}` — Full customer dossier (subscription, usage, invoices, logs)
* `PATCH /api/v1/admin/users/{id}/status` — Change user status (`ACTIVE`, `SUSPENDED`, `DEACTIVATED`)
* `GET /api/v1/admin/plans` — List all platform plans
* `PUT /api/v1/admin/plans/{id}` — Update future plan pricing and limits
* `GET /api/v1/admin/subscriptions` — Platform-wide subscriptions registry
* `GET /api/v1/admin/payments` — All gateway payment orders ledger
* `GET /api/v1/admin/invoices` — Master tax invoices registry
* `GET /api/v1/admin/audit-logs` — Security audit trail
* `GET /api/v1/admin/health` — Live JVM heap telemetry and system health

# TARGET ARCHITECTURE SPECIFICATION — CONSUMER AI SaaS

**Document**: `docs/TARGET_ARCHITECTURE.md`  
**Application**: Consumer AI SaaS Platform  
**Target Roles**: `USER`, `ADMIN`  
**Architecture Paradigm**: Direct User-Owned Single-Tenant Domain Model  

---

## 1. System Topology

```text
                             AI SaaS PLATFORM
                                    │
                     ┌──────────────┴──────────────┐
                     │                             │
                   USER                          ADMIN
                     │                             │
        ┌────────────┼────────────┐       ┌────────┼────────┐
        │            │            │       │        │        │
     AI Usage   Subscription   Billing  Users    Plans   Analytics
        │            │            │       │        │        │
        └────────────┴────────────┘    Payments  Audit    Health
                     │                             │
             Personal Invoices             Platform Insights
```

---

## 2. Target Domain Model & Entity Ownership

### Entity Hierarchy
Every operational domain entity is directly owned by `User`:
```text
User (id, email, passwordHash, firstName, lastName, globalRole, status, timestamps)
 │
 ├── Subscription (user_id FK, plan_id FK, status, periodStart, periodEnd, cancelAtPeriodEnd, etc.)
 ├── UsageRecord (user_id FK, metric, quantity, periodStart, periodEnd, metadata, createdAt)
 ├── PaymentOrder (user_id FK, plan_id FK, billingInterval, amount, currency, status, gateway details)
 ├── Invoice (user_id FK, subscription_id FK, invoiceNumber, subtotal, taxAmount, totalAmount, status)
 │    └── InvoiceItem (invoice_id FK, description, quantity, unitPrice, amount)
 ├── Notification (user_id FK, type, title, message, isRead, readAt, metadata, createdAt)
 ├── RefreshToken (user_id FK, tokenHash, expiresAt, revoked, createdAt)
 └── AuditLog (user_id Nullable FK, userEmail, action, entityType, entityId, details, ipAddress, createdAt)
```

Global Plan definitions remain independent:
```text
Plan (id, code, name, priceMonthly, priceYearly, currency, monthlyAiLimit, storageLimitMb, isActive)
 ├── FREE (0 price, 50 AI requests/mo)
 ├── PRO (₹2,900/mo or ₹29,000/yr, 5,000 AI requests/mo)
 └── BUSINESS (₹9,900/mo or ₹99,000/yr, 50,000 AI requests/mo)
```

---

## 3. Role & Permission Model

### Two Platform Roles
1. **`ROLE_USER`**: Individual consumer.
   - Registers directly, automatically receives `FREE` subscription upon signup.
   - Uses AI Studio, triggers AI completions via `/api/v1/ai/generate` or `/api/v1/ai/chat`.
   - Views personal usage meter, quota thresholds, and billing cycle.
   - Upgrades/cancels personal subscription using Razorpay or Sandbox gateway.
   - Views and prints personal GST tax invoices.
   - Receives personal notifications (billing, quota alerts at 75%, 90%, 100%, security alerts).
   - Manages personal profile, changes password (with active session revocation), and reviews active sessions.
   - **Access to `/api/v1/admin/**` returns HTTP 403 Forbidden**.

2. **`ROLE_ADMIN`**: Platform owner / operator / maintenance administrator.
   - Global dashboard overview (total users, active subscriptions, ARR, MRR, revenue, AI volume, system health).
   - User Management: search, paginate, inspect user profile and history, suspend/activate/deactivate users.
   - Subscription & Plan Management: inspect all subscriptions, update future plan prices and quotas (with audit trail; historical invoices untouched).
   - Financial & Payment Inspection: inspect all payment orders, transactions, and tax invoices.
   - Authoritative SaaS Analytics: MRR, ARR, conversion rate, churn rate, ARPPU, usage volume, gateway success rate.
   - System Audit Trail: paginated security and system audit log viewer.
   - System Health: CPU/memory metrics, DB connection status, active thread pool state.

---

## 4. AI Usage Architecture & Provider Abstraction

```text
User Request (Prompt / Message)
             │
             ▼
     AI Controller (/api/v1/ai/generate or /api/v1/ai/chat)
             │
             ▼
     AI Service
             │
             ▼
  1. Quota Check (Atomic Pessimistic Lock in UsageService)
     ├── If quota exceeded: Throw QuotaExceededException -> HTTP 429
     └── If quota available: Proceed
             │
             ▼
  2. AI Provider Abstraction (`AiProvider` interface)
     ├── OpenAI Provider / Gemini Provider / Mock Provider
     └── Generate Response
             │
             ▼
  3. Record Usage (UsageRecord with quantity and timestamp)
             │
             ▼
  4. Quota Threshold Alert Check (Idempotent 75%, 90%, 100% notification triggers)
             │
             ▼
  Return AI Response Payload to User
```

---

## 5. Security & Invariant Rules

1. **Strict User-Scoped Queries**: Normal user endpoints NEVER take `userId` or `organizationId` from client parameters. All domain operations extract `userId` from the authenticated `UserPrincipal`.
2. **Server-Side Financial Calculations**: Price, tax (18% GST), and totals are strictly determined by querying the `Plan` table server-side.
3. **Webhook Cryptographic Integrity**: Razorpay HMAC-SHA256 signatures are validated and idempotency is enforced via `WebhookEvent` table.
4. **Pessimistic Quota Locking**: Quota checks and increments use `SELECT ... FOR UPDATE` row-level locking or synchronized atomic queries to prevent race-condition quota bypass.
5. **Rate Limiting Protection**: High-risk endpoints (`/auth/login`, `/auth/register`, `/auth/refresh`, `/users/me/change-password`, `/billing/orders/**`, `/billing/webhook`) are guarded by rate limiters returning HTTP 429.
6. **Audit Trail Persistence**: Sensitive events (authentication, password changes, subscription changes, payment actions, admin changes) generate immutable audit records.

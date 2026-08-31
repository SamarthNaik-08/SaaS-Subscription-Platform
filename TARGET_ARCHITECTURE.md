# TARGET ARCHITECTURE SPECIFICATION (Phase 4 Consumer AI SaaS)

## 1. System Overview
The target architecture is a **consumer-oriented AI SaaS platform** where an individual customer (`USER`) directly owns their subscription, usage records, payments, invoices, and settings. A platform-level `ADMIN` manages users, plans, system health, and analytics.

```
                         AI SAAS PLATFORM
                                │
                 ┌──────────────┴──────────────┐
                 │                             │
               USER                          ADMIN
                 │                             │
       ┌─────────┼─────────┐          ┌────────┼────────┐
       │         │         │          │        │        │
    AI Usage Subscription Billing    Users   Plans   Analytics
       │                   │          │        │        │
       └───────────────────┘          Payments Audit System
```

## 2. Target Entity Relationship Model
```
┌─────────────────────────────────────────────────────────────┐
│                            User                             │
│  - id: UUID (PK)                                            │
│  - email: String (Unique)                                   │
│  - passwordHash: String                                     │
│  - firstName, lastName: String                              │
│  - role: GlobalRole (USER, ADMIN)                           │
│  - status: UserStatus (ACTIVE, SUSPENDED, DEACTIVATED)      │
│  - createdAt, updatedAt, lastLoginAt: Timestamp             │
└──────────────┬───────────────────────────────┬──────────────┘
               │ (1:1)                         │ (1:N)
               ▼                               ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│         Subscription         │ │         UsageRecord          │
│  - id: UUID (PK)             │ │  - id: UUID (PK)             │
│  - user_id: UUID (FK)        │ │  - user_id: UUID (FK)        │
│  - plan_id: UUID (FK)        │ │  - metric: UsageMetric       │
│  - status: SubscriptionStatus│ │  - quantity: Long            │
│  - currentPeriodStart/End    │ │  - periodStart/End: Timestamp│
│  - cancelAtPeriodEnd: bool   │ └──────────────────────────────┘
└──────────────┬───────────────┘
               │ (1:N)                         │ (1:N)
               ▼                               ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│         PaymentOrder         │ │           Invoice            │
│  - id: UUID (PK)             │ │  - id: UUID (PK)             │
│  - user_id: UUID (FK)        │ │  - user_id: UUID (FK)        │
│  - plan_id: UUID (FK)        │ │  - subscription_id: UUID (FK)│
│  - billingInterval           │ │  - invoiceNumber: String(UQ) │
│  - amount: BigDecimal        │ │  - subtotal, tax, total      │
│  - gatewayOrderId/PaymentId  │ │  - status: InvoiceStatus     │
└──────────────┬───────────────┘ └──────────────┬───────────────┘
               │                                │ (1:N)
               ▼                                ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│       WebhookEvent           │ │         InvoiceItem          │
│  - (provider, event_id) (UQ) │ │  - description, qty, price   │
└──────────────────────────────┘ └──────────────────────────────┘
```

## 3. Platform Roles & Authorization Matrix
1. `USER`:
   - Directly consumes AI endpoints (`/api/v1/usage/**`).
   - Manages personal subscription (`/api/v1/subscription/**`).
   - Initiates & verifies personal payments (`/api/v1/billing/**`).
   - Retrieves personal invoices (`/api/v1/invoices/**`).
   - Manages personal profile and sessions (`/api/v1/users/me/**`).
2. `ADMIN`:
   - Platform operator with access to `/api/v1/admin/**`.
   - Global user management, plan management, financial analytics, and audit log inspection.

# CURRENT ARCHITECTURE AUDIT REPORT (Phases 1–3 Baseline)

**Document**: `docs/CURRENT_ARCHITECTURE.md`  
**Application**: Consumer AI SaaS Platform (Pre-Migration State)  
**Audit Date**: August 2026  
**Auditor**: Architecture, Security & Engineering Team  

---

## 1. Executive Summary & Status Classification

The platform was originally developed under a multi-tenant B2B organizational paradigm. This document catalogs the actual implemented state across backend, frontend, database schema, security, and tests before migration to the pure **Consumer AI SaaS** model.

### Component Implementation Status Matrix

| Component | Architecture Role | Actual Code State | Classification |
| :--- | :--- | :--- | :--- |
| **`User` & Auth** | User identity, BCrypt hashing, JWT + Refresh Token Rotation | Fully implemented in `com.saasplatform.auth` and `com.saasplatform.user` | **IMPLEMENTED (Retain & Refactor Role)** |
| **`GlobalRole`** | Enum with `USER`, `SUPER_ADMIN` | Implemented in `GlobalRole.java` | **PARTIALLY IMPLEMENTED (Refactor to `USER`, `ADMIN`)** |
| **`Organization`** | B2B tenant container | Implemented in `com.saasplatform.organization` | **OBSOLETE (Delete)** |
| **`OrganizationMember`** | User-tenant junction with `OWNER`/`ADMIN`/`MEMBER` | Implemented in `com.saasplatform.organization` | **OBSOLETE (Delete)** |
| **`OrganizationInvitation`** | Team invitation tokens and validation | Implemented in `com.saasplatform.organization` | **OBSOLETE (Delete)** |
| **`Plan`** | Global plan definitions (`FREE`, `PRO`, `BUSINESS`) | Implemented in `com.saasplatform.plan` | **IMPLEMENTED (Retain & Refactor fields)** |
| **`Subscription`** | Billing subscriptions bound to `organization_id` | Implemented in `com.saasplatform.subscription` | **PARTIALLY IMPLEMENTED (Migrate to `user_id`)** |
| **`UsageRecord`** | Atomic pessimistic locked quota tracking | Implemented in `com.saasplatform.usage` | **PARTIALLY IMPLEMENTED (Migrate to `user_id`)** |
| **`PaymentOrder`** | Razorpay / Sandbox order creation & signature verification | Implemented in `com.saasplatform.billing` | **PARTIALLY IMPLEMENTED (Migrate to `user_id`)** |
| **`Invoice` / `InvoiceItem`** | Sequential GST tax invoice generation | Implemented in `com.saasplatform.billing` | **PARTIALLY IMPLEMENTED (Migrate to `user_id`)** |
| **`WebhookEvent`** | HMAC-SHA256 signature verification & idempotent processing | Implemented in `com.saasplatform.billing` | **IMPLEMENTED (Retain & Refactor references)** |
| **Admin Subsystem** | Platform dashboard, user management, plan editing | Documented in previous plans, no backend code | **DOCUMENTED ONLY (Implement Phase 4)** |
| **Analytics Engine** | Authoritative MRR, ARR, Churn, Conversion, ARPPU | Documented in previous plans, no backend code | **DOCUMENTED ONLY (Implement Phase 4)** |
| **Persistent Audit Logs** | Security and administrative audit trail | Documented in previous plans, no backend code | **DOCUMENTED ONLY (Implement Phase 4)** |
| **User Notifications** | Idempotent quota warnings (75%, 90%, 100%), billing alerts | Documented in previous plans, no backend code | **DOCUMENTED ONLY (Implement Phase 4)** |
| **User Settings & Sessions** | Self-serve profile, password change, session revocation | Documented in previous plans, no backend code | **DOCUMENTED ONLY (Implement Phase 4)** |
| **Rate Limiting** | Endpoint request throttling with HTTP 429 | Documented in previous plans, no backend code | **DOCUMENTED ONLY (Implement Phase 4)** |
| **AI Provider Abstraction** | Decoupled AI provider interface & pipeline | Documented in previous plans, no backend code | **DOCUMENTED ONLY (Implement Phase 4)** |

---

## 2. Backend Architecture (Current State)

### Packages & Modules
```text
com.saasplatform
 ├── auth            -> Registration, Login, Refresh Token Rotation (JWT + SHA-256 tokens)
 ├── billing         -> PaymentOrder, Invoice, InvoiceItem, InvoiceSequence, WebhookEvent, TaxCalculation, Gateways
 ├── common          -> Enums (GlobalRole, OrganizationRole, PlanCode, SubscriptionStatus, etc.), Utilities, ApiResponse
 ├── config          -> SecurityConfig, CorsConfig, PasswordConfig
 ├── exception       -> GlobalExceptionHandler, BadRequestException, ConflictException, ForbiddenException, etc.
 ├── organization    -> Organization, OrganizationMember, OrganizationInvitation, OrganizationService, etc.
 ├── plan            -> Plan, PlanRepository, PlanController, PlanInitializer (Seed FREE, PRO, BUSINESS)
 ├── refresh         -> RefreshToken, RefreshTokenRepository
 ├── security        -> CustomUserDetailsService, JwtAuthenticationFilter, JwtService, UserPrincipal
 ├── subscription    -> Subscription, SubscriptionRepository, SubscriptionService, SubscriptionAccessService
 ├── usage           -> UsageRecord, UsageRecordRepository, UsageService, UsageController
 └── user            -> User, UserRepository, UserController, UserDto
```

### Critical Architecture Flaws in Current State
1. **Organizational Coupling**: All core entities (`Subscription`, `UsageRecord`, `PaymentOrder`, `Invoice`) require `organization_id`. Normal users are forced to create and switch organizations upon signup.
2. **Horizontal Access Checks**: Every service call performs `verifyMembership(organizationId, user.getId())`, adding unnecessary database overhead and potential authorization leak surfaces.
3. **Missing Platform Admin Role**: Only `SUPER_ADMIN` existed in `GlobalRole`, but no dedicated `/api/v1/admin/**` controllers or administrative management workflows were implemented.
4. **Missing Production Safeguards**: No rate limiting filter was active; no persistent audit logging was captured; notifications were not persisted in the database.

---

## 3. Frontend Architecture (Current State)

### Existing Structure
- **Contexts**: `AuthContext.jsx` (JWT, user state), `OrganizationContext.jsx` (current org, member list, org switcher).
- **Hooks**: `useAuth.js`, `useOrganization.js`.
- **Pages**:
  - `LandingPage.jsx`, `LoginPage.jsx`, `RegisterPage.jsx`
  - `DashboardPage.jsx`, `AIStudioPage.jsx`, `UsagePage.jsx`, `SubscriptionPage.jsx`, `BillingPage.jsx`, `ProfilePage.jsx`
  - `OrganizationPage.jsx`, `TeamPage.jsx`, `AcceptInvitationPage.jsx` (Obsolete B2B pages)
- **Services**: `api.js`, `authService.js`, `billingService.js`, `usageService.js`, `organizationService.js`, `memberService.js`, `invitationService.js`.

### Frontend B2B Dependencies
- Every authenticated page subscribes to `OrganizationContext`.
- Headers send `X-Organization-Id` or retrieve `currentOrg.id`.
- Navigation displays team management and organization settings tabs.

---

## 4. Current Database Schema

```sql
-- Core User & Auth
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    global_role VARCHAR(50) NOT NULL, -- USER, SUPER_ADMIN
    status VARCHAR(50) NOT NULL,      -- ACTIVE, SUSPENDED, DEACTIVATED
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP
);

-- Obsolete B2B Tables
CREATE TABLE organizations (...);
CREATE TABLE organization_members (...);
CREATE TABLE organization_invitations (...);

-- Billing & Quota (Coupled to organization_id)
CREATE TABLE plans (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    price_monthly NUMERIC(10,2) NOT NULL,
    price_yearly NUMERIC(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    monthly_ai_limit INT NOT NULL,
    max_members INT NOT NULL, -- Obsolete
    storage_limit_mb INT NOT NULL,
    is_active BOOLEAN NOT NULL
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id), -- Needs migration to user_id
    plan_id UUID NOT NULL REFERENCES plans(id),
    status VARCHAR(50) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    cancel_at_period_end BOOLEAN NOT NULL,
    cancelled_at TIMESTAMP,
    payment_provider VARCHAR(50),
    external_subscription_id VARCHAR(255)
);

CREATE TABLE usage_records (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id), -- Needs removal
    user_id UUID NOT NULL REFERENCES users(id),
    metric VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE payment_orders (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id), -- Needs removal
    user_id UUID NOT NULL REFERENCES users(id),
    plan_id UUID NOT NULL REFERENCES plans(id),
    billing_interval VARCHAR(50) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    gateway_provider VARCHAR(50) NOT NULL,
    gateway_order_id VARCHAR(255) NOT NULL,
    gateway_payment_id VARCHAR(255),
    gateway_signature VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    invoice_number VARCHAR(100) UNIQUE NOT NULL,
    organization_id UUID NOT NULL REFERENCES organizations(id), -- Needs migration to user_id
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    payment_order_id UUID REFERENCES payment_orders(id),
    subtotal NUMERIC(10,2) NOT NULL,
    tax_amount NUMERIC(10,2) NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    billing_period_start TIMESTAMP NOT NULL,
    billing_period_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

---

## 5. Test Suite Baseline
- **Backend Tests**: 61 tests executed via Maven, 61 passed, 0 failures, 0 errors.
- **Frontend Build**: Vite + React production build compiles cleanly into `dist/`.
- **Target of Migration**: Maintain 100% test passing rate and valid build with all new user-owned domain tests and Phase 4 features.

# STEP-BY-STEP REFACTORING & MIGRATION PLAN

## Overview
This migration plan details the step-by-step transformation from the legacy multi-tenant organization model to the direct **User-Owned Architecture** and the subsequent implementation of Phase 4 (Admin Panel, Analytics, Notifications, Audit Logging, Settings, and Rate Limiting).

---

## Phase A: Backend Entity & Repository Migration
1. **`GlobalRole`**: Standardize to `USER`, `ADMIN`.
2. **`Subscription`**:
   - Replace `organization` FK with `user` FK (`@ManyToOne User user` or `@OneToOne`).
   - Add `@Index` on `user_id`.
3. **`UsageRecord`**:
   - Replace `organization` FK with `user` FK (`@ManyToOne User user`).
   - Update composite index: `(user_id, metric, period_start, period_end)`.
4. **`PaymentOrder`**:
   - Remove `organization` FK (already has `user` FK).
5. **`Invoice`**:
   - Replace `organization` FK with `user` FK (`@ManyToOne User user`).
6. **Remove Obsolete Entities & Repositories**:
   - Remove `Organization`, `OrganizationMember`, `OrganizationInvitation`, `OrganizationRole`, `OrganizationStatus`, `InvitationStatus`.
   - Remove `OrganizationRepository`, `OrganizationMemberRepository`, `OrganizationInvitationRepository`.

---

## Phase B: Service & Controller Refactoring
1. **`AuthService`**:
   - `register`: Saves `User`, creates default `FREE` `Subscription` directly for the user, issues JWT and refresh tokens. (No organization created).
2. **`SubscriptionService` / `SubscriptionAccessService`**:
   - Migrate methods from `organizationId` to `userId`:
     - `upgradeSubscription(UUID userId, Plan targetPlan, BillingInterval interval, PaymentOrder order)`
     - `cancelSubscription(UUID userId)`
     - `resumeSubscription(UUID userId)`
     - `getSubscription(UUID userId)`
     - `checkQuota(UUID userId, UsageMetric metric, long requestedAmount)`
3. **`UsageService`**:
   - `recordUsage(UUID userId, UsageMetric metric, long quantity)`: Executes pessimistic locking on user subscription quota.
   - `getCurrentUsage(UUID userId)`, `getUsageHistory(UUID userId)`.
4. **`PaymentOrderService`**:
   - `createPaymentOrder(UUID userId, CreatePaymentOrderRequest request)`
   - `verifyPayment(UUID userId, VerifyPaymentRequest request)`
   - `settlePayment(PaymentOrder order, String paymentId, String signature)`
5. **`InvoiceService`**:
   - `generateInvoice(User user, Subscription sub, PaymentOrder order, Plan plan, BillingInterval interval)`
   - `getInvoices(UUID userId)`, `getInvoiceById(UUID userId, UUID invoiceId)`
6. **Controllers**:
   - Update `SubscriptionController`, `UsageController`, `BillingController`, `InvoiceController` to extract `userPrincipal.getId()` directly.
   - Remove `OrganizationController`, `OrganizationMemberController`, `InvitationController`.

---

## Phase C: Phase 4 Implementation
1. **Admin Subsystem**:
   - `AdminController` (`/api/v1/admin/**` protected by `ROLE_ADMIN`).
   - `AdminAnalyticsService`: Authoritative metrics for MRR, ARR, churn, conversions, usage, revenue.
   - `AdminManagementService`: Paginated user management, plan editing, payment/invoice visibility.
2. **Audit Logging**:
   - `AuditLog` entity, repository, and `AuditLogService`.
   - Records auth, subscription, billing, usage, and admin operations.
3. **Notification System**:
   - `Notification` entity, repository, and `NotificationService`.
   - Idempotent threshold notifications (75%, 90%, 100%) and email logging service fallback.
4. **User Settings**:
   - `UserController` (`/api/v1/users/me/**`): profile edit, password change with BCrypt, session revocation.
5. **Rate Limiting & Security Hardening**:
   - In-memory sliding window rate limiter filter for auth and billing endpoints (`HTTP 429`).
   - Security headers (CSP, X-Content-Type-Options, Referrer-Policy, Frame-Options).

---

## Phase D: Frontend Refactoring & Admin Dashboard
1. **Clean up context**:
   - Remove `OrganizationContext.jsx`.
   - Update `AuthContext.jsx` to maintain authenticated user state and active subscription.
2. **User Navigation**:
   - Dashboard, AI Studio, Usage, Subscription, Billing, Notifications, Settings.
3. **Admin Dashboard (`/admin`)**:
   - Protected by `AdminProtectedRoute` (`role === 'ADMIN'`).
   - Dedicated Admin Layout with Dashboard KPIs, Users, Subscriptions, Usage, Payments, Invoices, Plans, Audit Logs, System Health.
4. **Settings & Notification Bell**:
   - `SettingsPage.jsx` with Profile, Security (password change & active sessions), Notifications tabs.
   - `NotificationBell.jsx` in header with unread badge and mark-read dropdown.

---

## Phase E: Automated Test Suite & Verification
1. Update existing test classes to user-level fixtures.
2. Add new unit and integration tests for Admin, Audit Logs, Notifications, User Settings, Rate Limiting.
3. Verify `mvn clean test` and `npm run build`.

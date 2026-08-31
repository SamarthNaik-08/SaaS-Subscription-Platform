# MIGRATION PLAN — B2B MULTI-TENANT TO CONSUMER AI SaaS

**Document**: `docs/MIGRATION_PLAN.md`  
**Migration Scope**: Backend Domain Model, Database Schema, Security Layer, Phase 4 Modules, Frontend Navigation & UI  

---

## 1. Migration Dependency Graph & Step Sequencing

```text
Step 1: Audit & Document (Current State vs Target State)
   │
Step 2: Backend Core Refactoring
   ├── GlobalRole enum (`USER`, `ADMIN`)
   ├── User entity & UserPrincipal
   ├── Plan entity (remove `maxMembers`)
   ├── Migrate Subscription (replace `organization` with `user`)
   ├── Migrate UsageRecord (replace `organization` with `user`)
   ├── Migrate PaymentOrder (replace `organization` with `user`)
   └── Migrate Invoice (replace `organization` with `user`)
   │
Step 3: Remove Obsolete B2B Components
   ├── Remove `com.saasplatform.organization` package and controllers
   ├── Remove `OrganizationRole`, `OrganizationStatus`, `InvitationStatus`
   └── Remove `SubscriptionAccessService` (replaced by direct auth principal checks)
   │
Step 4: Update Existing Core Services & Controllers
   ├── `AuthService`: Auto-assign FREE subscription on user registration (in 1 transaction)
   ├── `SubscriptionService`: User-scoped billing, upgrades, cancellations
   ├── `UsageService`: User-scoped pessimistic locked quota checking
   ├── `PaymentOrderService` & `InvoiceService`: User-scoped checkout and invoice generation
   └── `UsageController`, `SubscriptionBillingController`, `InvoiceController`: User-scoped APIs
   │
Step 5: Implement Phase 4 Systems
   ├── Persistent `AuditLog` (Entity, Repository, Service, Event listener/hooks)
   ├── User `Notification` System (Entity, Repository, Service, Controller, Quota warnings)
   ├── User Settings & Session Management (`/api/v1/users/me/**`, password change, token revoke)
   ├── AI Provider Abstraction (`AiProvider`, `MockAiProvider`, `AiService`, `AiController`)
   ├── Platform `ADMIN` Subsystem (`AdminController`, `AdminService` for Users, Plans, Analytics, Health)
   ├── Authoritative Analytics Engine (MRR, ARR, Churn, Conversion, ARPPU calculation)
   └── Rate Limiting Filter (In-memory bucket for auth, billing, webhooks with 429 response)
   │
Step 6: Frontend Migration
   ├── Delete `OrganizationContext`, `useOrganization`, `organizationService`, etc.
   ├── Delete B2B pages (`OrganizationPage`, `TeamPage`, `AcceptInvitationPage`)
   ├── Update `Navbar`, `Sidebar`, `DashboardLayout` for Consumer navigation + Admin portal
   ├── Implement `/admin/**` pages (Dashboard, Users, Subscriptions, Payments, Invoices, Plans, Analytics, Audit Logs, Health)
   ├── Implement User Settings page, Notifications popover/page, and AI Studio
   └── Secure routes with role checks (`role === 'ADMIN'`)
   │
Step 7: Test Suite & Verification
   ├── Refactor all unit & integration tests to user-owned domain model
   ├── Add comprehensive Phase 4 tests (Admin security 403/200, Quota 429, Password change session revoke, Notifications, Analytics)
   ├── Execute `mvn clean test` (verify 100% pass)
   ├── Execute `npm run build` (verify clean build)
   └── Generate `docs/PHASE4_FINAL_AUDIT_REPORT.md`
```

---

## 2. Obsolete B2B Artifacts Removal Checklist

### Backend Files to Remove
- `com/saasplatform/organization/entity/Organization.java`
- `com/saasplatform/organization/entity/OrganizationMember.java`
- `com/saasplatform/organization/entity/OrganizationInvitation.java`
- `com/saasplatform/organization/repository/OrganizationRepository.java`
- `com/saasplatform/organization/repository/OrganizationMemberRepository.java`
- `com/saasplatform/organization/repository/OrganizationInvitationRepository.java`
- `com/saasplatform/organization/service/OrganizationService.java`
- `com/saasplatform/organization/service/MemberManagementService.java`
- `com/saasplatform/organization/service/InvitationService.java`
- `com/saasplatform/organization/controller/OrganizationController.java`
- `com/saasplatform/organization/controller/OrganizationMemberController.java`
- `com/saasplatform/organization/controller/InvitationController.java`
- `com/saasplatform/organization/dto/*` (All organization and invitation DTOs)
- `com/saasplatform/common/enums/OrganizationRole.java`
- `com/saasplatform/common/enums/OrganizationStatus.java`
- `com/saasplatform/common/enums/InvitationStatus.java`
- `com/saasplatform/common/utils/SlugUtils.java` (if only used by organizations)
- `com/saasplatform/subscription/service/SubscriptionAccessService.java`

### Frontend Files to Remove
- `frontend/src/context/OrganizationContext.jsx`
- `frontend/src/hooks/useOrganization.js`
- `frontend/src/services/organizationService.js`
- `frontend/src/services/memberService.js`
- `frontend/src/services/invitationService.js`
- `frontend/src/pages/Organization/OrganizationPage.jsx`
- `frontend/src/pages/Team/TeamPage.jsx`
- `frontend/src/pages/Invitations/AcceptInvitationPage.jsx`

---

## 3. Database Schema Migration Plan

Because Hibernate DDL-auto is set to `update` (and testing runs on in-memory H2 with PostgreSQL in production), updating the JPA entities directly establishes the clean target schema:

1. **`users` table**: `global_role` column values become `USER` or `ADMIN`.
2. **`plans` table**: drop/ignore obsolete `max_members` column.
3. **`subscriptions` table**: change foreign key column `organization_id` -> `user_id` (REFERENCES `users(id)`).
4. **`usage_records` table**: drop foreign key `organization_id`; retain `user_id` (REFERENCES `users(id)`).
5. **`payment_orders` table**: drop foreign key `organization_id`; retain `user_id` (REFERENCES `users(id)`).
6. **`invoices` table**: change foreign key `organization_id` -> `user_id` (REFERENCES `users(id)`).
7. **New Tables**:
   - `notifications` (`id`, `user_id`, `type`, `title`, `message`, `is_read`, `read_at`, `metadata`, `created_at`)
   - `audit_logs` (`id`, `user_id`, `user_email`, `action`, `entity_type`, `entity_id`, `details`, `ip_address`, `created_at`)

---

## 4. Financial & Analytics Formulas Specification

- **MRR (Monthly Recurring Revenue)**:
  $$\text{MRR} = \sum (\text{Active Monthly Plan Price}) + \sum \left(\frac{\text{Active Yearly Plan Price}}{12}\right)$$
- **ARR (Annual Recurring Revenue)**:
  $$\text{ARR} = \text{MRR} \times 12$$
- **Total Revenue**: Sum of all `PaymentOrder` records with `status = 'PAID'` (excluding GST / or recording gross revenue accurately as recorded in settled orders).
- **Conversion Rate**: $\frac{\text{Active Paying Subscribers (PRO + BUSINESS)}}{\text{Total Registered Users}} \times 100\%$
- **Churn Rate**: $\frac{\text{Cancelled Subscriptions in Period}}{\text{Total Subscriptions at Start of Period}} \times 100\%$
- **ARPPU (Average Revenue Per Paying User)**: $\frac{\text{Total MRR}}{\text{Active Paying Subscribers}}$

---

## 5. Security Invariants Verification Strategy

- **Tenant Isolation -> User Isolation**: Verify that `User A` cannot read or modify `User B`'s subscription, usage, invoices, payments, notifications, or sessions.
- **Admin Boundary**: Verify that `ROLE_USER` receives HTTP 403 on `/api/v1/admin/**` while `ROLE_ADMIN` has authorized access.
- **Quota Enforcement**: Verify that exceeding monthly quota throws `QuotaExceededException` (HTTP 429) under high concurrent load.
- **Password Revocation**: Verify that changing password immediately invalidates all existing refresh tokens.

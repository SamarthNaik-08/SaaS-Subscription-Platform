# CURRENT ARCHITECTURE SPECIFICATION (Phases 1–3)

## 1. System Overview
The existing system was built as a multi-tenant B2B platform where Organizations act as the fundamental container for Subscriptions, Quotas, Usage, Invoices, and Payment Orders.

```
                          ┌───────────────────────────┐
                          │           User            │
                          └─────────────┬─────────────┘
                                        │
                                        ▼ (1:N)
                          ┌───────────────────────────┐
                          │    OrganizationMember     │ (Role: OWNER / ADMIN / MEMBER)
                          └─────────────┬─────────────┘
                                        │
                                        ▼ (N:1)
                          ┌───────────────────────────┐
                          │       Organization        │
                          └─────────────┬─────────────┘
                                        │
         ┌──────────────────────────────┼──────────────────────────────┐
         ▼                              ▼                              ▼
┌──────────────────┐          ┌───────────────────┐          ┌───────────────────┐
│   Subscription   │          │    UsageRecord    │          │   PaymentOrder    │
└──────────────────┘          └───────────────────┘          └─────────┬─────────┘
                                                                       │
                                                                       ▼
                                                             ┌───────────────────┐
                                                             │      Invoice      │
                                                             └───────────────────┘
```

## 2. Existing Database Schema
- `users`: `id`, `email`, `password_hash`, `first_name`, `last_name`, `global_role` (`USER`, `SUPER_ADMIN`), `status`, timestamps.
- `organizations`: `id`, `name`, `slug`, `owner_id`, `status`, timestamps.
- `organization_members`: `id`, `organization_id`, `user_id`, `role` (`OWNER`, `ADMIN`, `MEMBER`), `status`.
- `organization_invitations`: `id`, `organization_id`, `email`, `role`, `token_hash`, `status`, `expires_at`.
- `plans`: `id`, `code`, `name`, `price_monthly`, `price_yearly`, `currency`, `monthly_ai_limit`, `max_members`, `storage_limit_mb`, `is_active`.
- `subscriptions`: `id`, `organization_id`, `plan_id`, `status`, `start_date`, `current_period_start`, `current_period_end`, `cancel_at_period_end`, `cancelled_at`, `payment_provider`, `external_subscription_id`.
- `usage_records`: `id`, `organization_id`, `user_id`, `metric`, `quantity`, `period_start`, `period_end`, `metadata`.
- `payment_orders`: `id`, `organization_id`, `user_id`, `plan_id`, `billing_interval`, `amount`, `currency`, `status`, `gateway_provider`, `gateway_order_id`, `gateway_payment_id`, `gateway_signature`.
- `invoices`: `id`, `invoice_number`, `organization_id`, `subscription_id`, `payment_order_id`, `subtotal`, `tax_amount`, `total_amount`, `currency`, `status`, `billing_period_start`, `billing_period_end`.
- `invoice_items`: `id`, `invoice_id`, `description`, `quantity`, `unit_price`, `amount`.
- `invoice_sequences`: `period_key`, `last_sequence`.
- `webhook_events`: `id`, `provider`, `provider_event_id`, `event_type`, `payload_hash`, `status`, `failure_reason`.
- `refresh_tokens`: `id`, `user_id`, `token_hash`, `expires_at`, `revoked`.

## 3. Existing Access & Role Mechanics
- Global Roles: `USER`, `SUPER_ADMIN`.
- Organization Roles: `OWNER`, `ADMIN`, `MEMBER`.
- Every domain operation requires resolving the caller's active organization via `MemberManagementService` or `OrganizationService`.

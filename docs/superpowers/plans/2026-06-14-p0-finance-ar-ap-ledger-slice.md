# P0 Finance AR/AP Ledger Slice Plan

## 1. Objective

Implement the P0 finance ledger foundation on 192.168.88.160 with Java 21 + Spring Boot 4.x + PostgreSQL + Flyway.

This slice gives P0 a formal receivable/payable ledger, payment registration, payment intent placeholder, offline payment confirmation, refund record, daily close lock record, and reconciliation read models.

## 2. In Scope

Formalize these OpenAPI P0 paths:

- `GET /api/v1/finance/ar-ap-items`
- `POST /api/v1/finance/ar-ap-items`
- `POST /api/v1/finance/ar-ap-items/{id}/payments`
- `POST /api/v1/finance/daily-closes`
- `POST /api/v1/finance/payment-intents`
- `POST /api/v1/finance/payments/{payment_id}/confirm-offline`
- `GET /api/v1/finance/reconciliation/batches`
- `GET /api/v1/finance/reconciliation/differences`
- `POST /api/v1/finance/refunds`

## 3. Out of Scope

- General ledger, vouchers, tax filing, invoicing, and tax-control integration.
- Real aggregated payment provider API calls.
- Automatic settlement formulas, invoice automation, and full merchant billing portal.
- Complex dispute workflow and adjustment approval workflow.

## 4. Files To Add

- `backend/src/main/resources/db/migration/V008__p0_finance_ar_ap_ledger.sql`
- `backend/src/main/java/com/complex/platform/finance/FinanceController.java`
- `backend/src/main/java/com/complex/platform/finance/FinanceService.java`
- `backend/src/main/java/com/complex/platform/finance/FinanceRepository.java`
- `backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java`
- `backend/src/main/java/com/complex/platform/finance/*Record.java`
- `backend/src/main/java/com/complex/platform/finance/*Response.java`
- `backend/src/main/java/com/complex/platform/finance/dto/*Request.java`
- `backend/src/test/java/com/complex/platform/finance/FinanceControllerTests.java`
- `backend/src/test/java/com/complex/platform/finance/FinanceServiceTests.java`
- `backend/src/test/java/com/complex/platform/finance/FinanceJdbcIntegrationTests.java`

## 5. Data Model

Add PostgreSQL tables with idempotent Flyway DDL:

- `ar_ap_items`
  - `id`, `item_no`, `direction`, `counterparty_id`, `source_type`, `source_id`, `amount_cent`, `paid_amount_cent`, `unpaid_amount_cent`, `currency`, `due_date`, `accounting_period`, `status`, `created_at`, `updated_at`
  - Unique optional guard: `(direction, source_type, source_id)` when `source_id` is not null.
- `ar_ap_item_sources`
  - `id`, `ar_ap_item_id`, `source_type`, `source_id`, `amount_cent`, `created_at`
- `ar_ap_payments`
  - `id`, `ar_ap_item_id`, `amount_cent`, `payment_method`, `payment_time`, `bank_flow_ref`, `created_at`
- `finance_payment_intents`
  - `id`, `business_order_type`, `business_order_id`, `amount_cent`, `currency`, `channel`, `status`, `created_at`, `updated_at`
- `finance_offline_payment_confirmations`
  - `id`, `payment_id`, `amount_cent`, `payment_method`, `confirmed_at`, `evidence_attachment_ids`, `status`, `created_at`
- `finance_refunds`
  - `id`, `target_id`, `refund_amount_cent`, `reason`, `refund_channel`, `status`, `created_at`
- `finance_daily_closes`
  - `id`, `business_date`, `scope_type`, `scope_id`, `force_close`, `status`, `locked_at`, `created_at`
- `finance_reconciliation_batches`
  - `id`, `batch_no`, `scope_type`, `scope_id`, `business_date`, `status`, `created_at`
- `finance_reconciliation_differences`
  - `id`, `batch_id`, `difference_type`, `business_ref`, `amount_cent`, `status`, `created_at`

Seed permissions and attach to `role_platform_admin`:

- `finance.ar_ap.view`
- `finance.ar_ap.manage`
- `finance.payment.register`
- `finance.daily_close.lock`
- `finance.payment_intent.create`
- `finance.reconciliation.view`
- `openplatform.reconcile.execute`
- `finance.refund.create`

## 6. Business Rules

- All money fields are integer cents; never use floating point.
- `direction` must be `receivable` or `payable` for the OpenAPI request. Internal accounting may be added later but not accepted by this request unless OpenAPI is updated.
- `amount_cent` must be positive for creating ledger items, payment intents, payments, confirmations, and refunds.
- New AR/AP item defaults:
  - `paid_amount_cent = 0`
  - `unpaid_amount_cent = amount_cent`
  - `currency = CNY`
  - `status = pending_collection` for receivable
  - `status = pending_payment` for payable
- Payment registration reduces `unpaid_amount_cent`.
  - If paid amount reaches total amount, status becomes `settled`.
  - If paid amount is between 0 and total amount, status becomes `partial_paid`.
  - Overpayment must return `VALIDATION_FAILED`.
- Daily close creates a lock record only; it must not mutate ledger amounts in this slice.
- Payment intent returns a formal internal intent with `status=created`; no real provider call.
- Offline payment confirmation records evidence ids and status; no real bank/POS verification.
- Refund creates a formal refund record with `status=requested`.
- Reconciliation GET endpoints return persisted rows and write audit events; they may return empty lists if no batches/differences exist.

## 7. Interface Rules

- Every endpoint must call `AuthorizationService.requirePermission(...)` with exact OpenAPI permissions.
- All write endpoints with `x-idempotency-required: true` must reject missing/blank `idempotency_key` with `VALIDATION_FAILED`.
- All write operations must go through `IdempotencyService`.
- All SQL must use `NamedParameterJdbcTemplate` with named parameters.
- Responses may use P0 generic success/list shapes but must be stable for frontend/QA.

## 8. Audit Events

Use exact OpenAPI event names:

- `finance_ar_ap.viewed` / `finance_ar_ap.view_failed`
- `finance_ar_ap.created` / `finance_ar_ap.create_failed`
- `finance_payment.registered` / `finance_payment.register_failed`
- `finance_daily_close.locked` / `finance_daily_close.lock_failed`
- `payment.intent_created` / `payment.intent_failed`
- `finance.reconciliation_batch.viewed` / `finance.reconciliation_batch.viewed_failed`
- `finance_reconciliation.difference_found` / `finance_reconciliation.view_failed`
- `finance_refund.created` / `finance_refund.create_failed`

## 9. TDD Order

1. Add failing controller tests:
   - AR/AP create missing `idempotency_key` returns `VALIDATION_FAILED`.
   - AR/AP list requires `finance.ar_ap.view`.
   - Payment registration requires `finance.payment.register`.
   - Payment intent requires `finance.payment_intent.create`.
   - Refund requires `finance.refund.create`.
2. Add failing service tests:
   - create receivable item computes unpaid amount and writes `finance_ar_ap.created`.
   - duplicate idempotency key does not create duplicate item.
   - partial payment updates ledger to `partial_paid`.
   - full payment updates ledger to `settled`.
   - overpayment fails with `VALIDATION_FAILED`.
   - daily close, payment intent, offline confirmation, refund write exact audit events.
3. Add failing JDBC integration tests:
   - V008 migration applies and permission seeds exist.
   - AR/AP create/list persists.
   - payment updates balances.
   - reconciliation endpoints can list empty/persisted rows.
4. Implement minimal controller/service/repository.
5. Run targeted tests.
6. Run full P0 verification.

## 10. Verification

Run:

- `cd /home/james/complex-management/backend && mvn -q -Dtest=FinanceServiceTests,FinanceControllerTests,FinanceJdbcIntegrationTests test`
- `/opt/complex/scripts/complex-verify-p0.sh`

Expected result:

- Finance targeted tests pass.
- Backend full tests pass.
- `verify:p0-contract` passes.
- Web/Android builds remain green.
- P0 verification finishes with `P0 verification PASS`.

## 11. Risk And Rollback

Risk:

- OpenAPI request bodies are intentionally minimal and do not yet include all fields from the AR/AP design document; do not add unsupported request fields in this slice.
- This slice is a finance ledger foundation, not a replacement for a full accounting system.
- Existing finance package only has `package-info.java`, so this creates the first formal finance code; keep class boundaries simple.

Rollback:

- Create a tar snapshot of `/home/james/complex-management` before editing.
- If full P0 verification fails and cannot be fixed quickly, restore the snapshot and redeploy the previous release.

# P1 Bar Table Session Adjustments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first P1 bar SaaS/POS enhancement for controlled card-seat/table session transfer, merge, and renewal without breaking the existing P0 bar order, bill, payment, stock, and verification flows.

**Architecture:** Add a P1 `bar_table_sessions` aggregate as the operational authority for opened card-seat/table usage, then append immutable `bar_table_session_adjustments` records for transfer, merge, and renewal. Existing P0 table/reservation/order/bill APIs remain backward-compatible; P1 endpoints are additive and enforce same bar, same merchant, same business date, idempotency, session version, permission, and audit events.

**Tech Stack:** Java 21, Spring Boot 4.1, PostgreSQL, Flyway, Spring JDBC, JUnit 5, Spring MVC tests, springdoc OpenAPI.

---

## Scope

Implement the first coding slice for `P1-BAR-01`:

- Create table sessions through `POST /api/v1/bar/table-sessions`.
- List sessions through `GET /api/v1/bar/table-sessions`.
- Transfer a session to another table through `POST /api/v1/bar/table-sessions/{session_id}/transfer`.
- Merge a secondary session into a primary session through `POST /api/v1/bar/table-sessions/{session_id}/merge`.
- Renew a session through `POST /api/v1/bar/table-sessions/{session_id}/renew`.
- Add permissions and role grants for platform admin.
- Write audit success/failure events for all three adjustment operations.
- Keep P0 endpoints and static P0 OpenAPI unchanged unless later P1 contract tooling is added.

## Explicit Defaults

- Cross-bar, cross-merchant, and cross-business-date operations are rejected.
- `merchant_id` is required when creating a P1 session because existing P0 `bar_tables` do not carry merchant ownership.
- Transfer requires target table to be `active`; after success, target table becomes `opened`, source table becomes `pending_clear`, and session `table_id` points to target.
- Merge requires both sessions to be active/open; the merged session becomes `merged`, its table becomes `pending_clear`, and the primary session version increments.
- Renewal appends a renewal adjustment. If the session is closed or settled in a later slice, the service must reject direct mutation; this first slice only supports active/open sessions.
- Operations require a non-blank `idempotency_key`, non-blank reason, operator id, and matching current `session_version`.
- The service stores JSON snapshots for auditability, but does not yet rewrite historical P0 order/bill rows; existing order/bill provenance remains intact.

## Files

Create:

- `backend/src/main/java/com/complex/platform/bar/BarTableSessionRecord.java`
- `backend/src/main/java/com/complex/platform/bar/BarTableSessionAdjustmentRecord.java`
- `backend/src/main/java/com/complex/platform/bar/BarTableSessionRequest.java`
- `backend/src/main/java/com/complex/platform/bar/BarTableTransferRequest.java`
- `backend/src/main/java/com/complex/platform/bar/BarTableMergeRequest.java`
- `backend/src/main/java/com/complex/platform/bar/BarTableRenewRequest.java`
- `backend/src/main/resources/db/migration/V019__p1_bar_table_session_adjustments.sql`

Modify:

- `backend/src/main/java/com/complex/platform/bar/BarController.java`
- `backend/src/main/java/com/complex/platform/bar/BarService.java`
- `backend/src/main/java/com/complex/platform/bar/BarRepository.java`
- `backend/src/main/java/com/complex/platform/bar/JdbcBarRepository.java`
- `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`
- `backend/src/test/java/com/complex/platform/bar/BarControllerTests.java`
- `backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java`

## Data Changes

Add `bar_table_sessions`:

- `id`, `bar_id`, `merchant_id`, `business_date`, `table_id`, `reservation_id`, `customer_id`
- `guest_count`, `min_consumption_snapshot_cent`, `allowed_pass_free_guest_count_snapshot`
- `sales_staff_id`, `service_staff_id`, `status`, `opened_at`, `closed_at`
- `source_session_id`, `session_version`, `created_at`, `updated_at`

Add `bar_table_session_adjustments`:

- `id`, `session_id`, `adjustment_type`, `source_session_id`, `target_session_id`
- `from_table_id`, `to_table_id`, `bar_id`, `merchant_id`, `business_date`
- `min_consumption_before_cent`, `min_consumption_after_cent`
- `renewed_until`, `additional_min_consumption_cent`, `additional_deposit_amount_cent`, `additional_service_fee_cent`
- `reason`, `requested_by_user_id`, `approval_required`, `status`, `idempotency_key`
- `evidence_attachment_ids`, `snapshot_json`, `created_at`

Add permissions:

- `bar.table_session.view`
- `bar.table_session.create`
- `bar.table_session.transfer`
- `bar.table_session.merge`
- `bar.table_session.renew`

## API Changes

Add P1 endpoints:

- `GET /api/v1/bar/table-sessions`
- `POST /api/v1/bar/table-sessions`
- `POST /api/v1/bar/table-sessions/{session_id}/transfer`
- `POST /api/v1/bar/table-sessions/{session_id}/merge`
- `POST /api/v1/bar/table-sessions/{session_id}/renew`

## Task 1: RED Service Tests

**Files:**

- Modify: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`

- [ ] Add failing tests for transfer success, cross-bar rejection, merge success, renewal success, and stale version rejection.
- [ ] Run on 160:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests test
```

Expected before implementation: compile failure because `BarTableSessionRequest`, `BarTableTransferRequest`, `BarTableMergeRequest`, `BarTableRenewRequest`, and service methods do not exist.

## Task 2: RED Controller Tests

**Files:**

- Modify: `backend/src/test/java/com/complex/platform/bar/BarControllerTests.java`

- [ ] Add route-permission rows for the five new P1 routes.
- [ ] Add missing idempotency checks for the four write routes.
- [ ] Run on 160:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarControllerTests test
```

Expected before implementation: compile failure because controller methods and DTOs do not exist.

## Task 3: RED JDBC Integration Tests

**Files:**

- Modify: `backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java`

- [ ] Add migration-backed tests proving sessions and adjustments persist and can be listed.
- [ ] Run on 160:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarJdbcIntegrationTests test
```

Expected before implementation: compile or SQL failure because migration, repository methods, and records do not exist.

## Task 4: Minimal Implementation

**Files:**

- Create the six Java records.
- Create `V019__p1_bar_table_session_adjustments.sql`.
- Modify repository, JDBC repository, service, and controller.

- [ ] Implement the smallest code that makes the RED tests pass.
- [ ] Use `IdempotencyService.execute(...)` for all writes.
- [ ] Use `AuditEventService.recordSuccess(...)` and `recordFailure(...)` around adjustment operations.
- [ ] Keep all SQL parameterized through `NamedParameterJdbcTemplate`.
- [ ] Do not alter P0 endpoint behavior.

## Task 5: Verification

Run on 160:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests test
mvn -Dtest=BarControllerTests test
mvn -Dtest=BarJdbcIntegrationTests test
cd /home/james/complex-management
npm run verify:p0-contract
npm run test:backend
```

Expected after implementation:

- The targeted bar tests pass.
- The full backend test suite passes.
- `verify:p0-contract` still passes, proving P1 additive work did not break the P0 contract gate.

## Acceptance Criteria

- Given an active P1 session and an active target table in the same bar, merchant, and business date, when a supervisor transfers the session with the correct version and idempotency key, then the session points to the target table, source table becomes `pending_clear`, target table becomes `opened`, adjustment record is persisted, and `bar.table.transferred` is written.
- Given the target table belongs to another bar, when transfer is submitted, then the service rejects with `BAR_SCOPE_MISMATCH`, no session/table mutation is committed, and a failure event is written.
- Given two active sessions in the same bar, merchant, and business date, when a supervisor merges the secondary session into the primary session, then the secondary session becomes `merged`, primary version increments, and `bar.table.merged` is written.
- Given an active session, when a supervisor renews it with a future `renewed_until`, then a renewal adjustment is persisted, session version increments, and `bar.table.renewed` is written.
- Given a stale `session_version`, when any adjustment is submitted, then the service rejects with `BAR_TABLE_SESSION_VERSION_CONFLICT`.

## Risks

- Existing P0 code has no merchant field on `bar_tables`; requiring `merchant_id` on P1 session creation is necessary for the same-merchant invariant.
- Historical orders and bills remain tied to P0 table/reservation fields in this slice; adjustment snapshots preserve traceability until a later P1 slice introduces session-linked order/bill rows.
- 160 project is not a Git repository, so rollback uses `/tmp/complex-baselines/p1-bar-session-before-*.tar.gz` plus local file copies instead of `git checkout`.

## Rollback

- Restore changed files from the latest 160 baseline tarball:

```bash
cd /home/james/complex-management
tar -xzf /tmp/complex-baselines/p1-bar-session-before-20260614190413.tar.gz
```

- If the Flyway migration was applied to a disposable local/dev database only, recreate the test database. Do not manually delete production migration rows.


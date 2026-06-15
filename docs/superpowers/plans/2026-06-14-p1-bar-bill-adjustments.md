# P1 Bar Bill Adjustments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the P1 bar bill adjustment slice for service fee, minimum-spend top-up, and on-account registration with auditable records.

**Architecture:** Keep the existing `BarBillRecord` as the authoritative bill snapshot and add a dedicated adjustment ledger for P1 bill changes. Service fee, minimum-spend top-up, and on-account actions update the bill through `BarService`, persist an immutable adjustment row, and emit audit events through the existing idempotency and audit-event services.

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL, Flyway, Maven, JUnit, MockMvc.

---

## File Map

- Modify: `backend/src/main/java/com/complex/platform/bar/BarService.java`
  - Add public bill adjustment APIs and internal validation.
  - Keep all writes wrapped in `executeWrite`.
- Modify: `backend/src/main/java/com/complex/platform/bar/BarController.java`
  - Add bill adjustment endpoints and exact permissions.
- Modify: `backend/src/main/java/com/complex/platform/bar/BarRepository.java`
  - Add bill adjustment persistence methods.
- Modify: `backend/src/main/java/com/complex/platform/bar/JdbcBarRepository.java`
  - Add JDBC save/list methods for adjustment rows.
- Create: `backend/src/main/java/com/complex/platform/bar/BarBillAdjustmentRecord.java`
  - Immutable adjustment ledger record.
- Create: `backend/src/main/java/com/complex/platform/bar/BarBillAdjustmentRequest.java`
  - Request DTO for service fee, minimum-spend top-up, and on-account actions.
- Create: `backend/src/main/resources/db/migration/V020__p1_bar_bill_adjustments.sql`
  - Add `bar_bill_adjustments` table and bill-adjustment permissions.
- Modify: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`
  - Add TDD coverage for service fee, low-spend top-up, on-account, and invalid adjustment types.
- Modify: `backend/src/test/java/com/complex/platform/bar/BarControllerTests.java`
  - Add route and idempotency coverage.
- Modify: `backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java`
  - Add Flyway 020, permission seed, and JDBC persistence coverage.
- Modify: cross-domain JDBC version tests that assert current Flyway version.
- Create: `docs/p1-bar-bill-adjustment-api-contract-v0.1.md`
  - Manual P1 API contract for this slice.

## Data And API Contract

### New Table

`bar_bill_adjustments`

| Column | Rule |
|---|---|
| `id` | Primary key, generated from adjustment type + idempotency key |
| `bill_id` | Required FK to `bar_bills.id` |
| `adjustment_type` | One of `service_fee`, `min_spend_top_up`, `on_account` |
| `amount_cent` | Required, non-negative; `on_account` uses 0 |
| `before_payable_amount_cent` | Required snapshot |
| `after_payable_amount_cent` | Required snapshot |
| `reason` | Required |
| `requested_by_user_id` | Required |
| `approval_required` | Required boolean |
| `status` | `applied` |
| `idempotency_key` | Required |
| `evidence_attachment_ids` | JSONB array |
| `snapshot_json` | JSONB object |
| `created_at` | Required timestamp |

### New Endpoint

`POST /api/v1/bar/bills/{bill_id}/adjustments`

Permission: `bar.bill.adjust`

Request:

```json
{
  "idempotency_key": "bill-adjust-once",
  "adjustment_type": "service_fee",
  "amount_cent": 12880,
  "reason": "10 percent service fee",
  "requested_by_user_id": "supervisor_1",
  "evidence_attachment_ids": ["att_1"]
}
```

Response: `BarBillAdjustmentRecord`.

## Task 1: Service Fee Adjustment

**Files:**
- Test: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`
- Create: `backend/src/main/java/com/complex/platform/bar/BarBillAdjustmentRecord.java`
- Create: `backend/src/main/java/com/complex/platform/bar/BarBillAdjustmentRequest.java`
- Modify: `backend/src/main/java/com/complex/platform/bar/BarService.java`
- Modify: `backend/src/main/java/com/complex/platform/bar/BarRepository.java`

- [ ] **Step 1: Write the failing service test**

Add a test named `serviceFeeAdjustmentIncreasesPayableAndWritesLedger`.

Expected behavior:
- Given a bill payable amount of `128800`.
- When `applyBillAdjustment` is called with `adjustmentType=service_fee` and `amountCent=12880`.
- Then the saved bill payable becomes `141680`.
- Then an adjustment row is saved with `before=128800`, `after=141680`, `status=applied`.
- Then audit event `bar.bill.adjusted` is recorded.

- [ ] **Step 2: Run RED**

Run on 160:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests test
```

Expected: compilation failure because adjustment record/request/service method do not exist.

- [ ] **Step 3: Implement minimal service fee support**

Add the two record classes, repository methods, and minimal `BarService.applyBillAdjustment`.

- [ ] **Step 4: Run GREEN**

Run:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests test
```

Expected: service fee test passes.

## Task 2: Minimum-Spend Top-Up And On-Account

**Files:**
- Test: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`
- Modify: `backend/src/main/java/com/complex/platform/bar/BarService.java`

- [ ] **Step 1: Write failing tests**

Add:
- `minimumSpendTopUpIncreasesPayableWithoutUsingTicketOrEntryRights`
- `onAccountMarksBillAsReceivableWithoutChangingPayable`
- `billAdjustmentRejectsUnsupportedType`

Expected behavior:
- `min_spend_top_up` increases payable by `amountCent` and writes adjustment type `min_spend_top_up`.
- `on_account` keeps payable unchanged, marks `paymentStatus=on_account`, marks `settlementStatus=receivable`, and writes amount `0`.
- Unsupported type throws `VALIDATION_FAILED`.

- [ ] **Step 2: Run RED**

Run:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests test
```

Expected: failures for missing behavior.

- [ ] **Step 3: Implement minimal branch logic**

Support exactly three adjustment types:
- `service_fee`
- `min_spend_top_up`
- `on_account`

No split-bill, merge-bill, or auto financial AR posting in this task.

- [ ] **Step 4: Run GREEN**

Run:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests test
```

Expected: all bar service tests pass.

## Task 3: Controller And Flyway

**Files:**
- Test: `backend/src/test/java/com/complex/platform/bar/BarControllerTests.java`
- Test: `backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java`
- Modify: `backend/src/main/java/com/complex/platform/bar/BarController.java`
- Modify: `backend/src/main/java/com/complex/platform/bar/JdbcBarRepository.java`
- Create: `backend/src/main/resources/db/migration/V020__p1_bar_bill_adjustments.sql`

- [ ] **Step 1: Write failing controller and JDBC tests**

Controller:
- `POST /api/v1/bar/bills/bill_1/adjustments` requires permission `bar.bill.adjust`.
- Missing `idempotency_key` returns `VALIDATION_FAILED`.

JDBC:
- `bar_bill_adjustments` table exists.
- `bar.bill.adjust` permission is seeded.
- `role_platform_admin` has the permission.
- Repository can save and list adjustments.
- Current Flyway version is `020`.

- [ ] **Step 2: Run RED**

Run:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarControllerTests,BarJdbcIntegrationTests test
```

Expected: compilation/failing tests until endpoint, migration, and JDBC methods are added.

- [ ] **Step 3: Implement controller, JDBC, and migration**

Add the endpoint, repository SQL, mapper, parameter source, table DDL, permission seed, and role grant.

- [ ] **Step 4: Run GREEN**

Run:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarControllerTests,BarJdbcIntegrationTests test
```

Expected: controller and JDBC tests pass.

## Task 4: Contract, Safety, And Full Verification

**Files:**
- Create: `docs/p1-bar-bill-adjustment-api-contract-v0.1.md`
- Modify: cross-domain JDBC integration tests if they assert current Flyway version.

- [ ] **Step 1: Add P1 contract document**

Include endpoint, permission, request fields, adjustment type semantics, error codes, and acceptance criteria.

- [ ] **Step 2: Run P0 contract gate**

Run:

```bash
cd /home/james/complex-management
npm run verify:p0-contract
```

Expected: pass, because P1 additions must not alter P0 OpenAPI artifacts.

- [ ] **Step 3: Run target and full backend tests**

Run:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests,BarControllerTests,BarJdbcIntegrationTests test
mvn test
```

Expected: all tests pass.

- [ ] **Step 4: Security and differential review**

Check:
- All write routes require idempotency key.
- All routes require exact permission.
- No dynamic SQL string concatenation.
- No hardcoded secrets.
- No ticket/pass/card-seat entry quota is used as a minimum-spend offset.


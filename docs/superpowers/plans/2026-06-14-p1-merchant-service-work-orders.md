# P1 Merchant Service Work Orders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the P1 merchant service/repair/complaint/rectification work-order backend loop so merchant-facing issues can be created, assigned, processed, closed, queried, and audited.

**Architecture:** Add an independent `merchantservice` Spring package instead of extending platform governance. The module stores only merchant/work-order facts and references merchant IDs, evidence attachment IDs, and staff IDs; it does not calculate rent/CAM/AR aging and does not integrate third-party service desks in this slice.

**Tech Stack:** Java 21, Spring Boot 4.1, PostgreSQL, Flyway, Spring JDBC, JUnit 5, MockMvc, existing idempotency and audit event services.

---

## Scope

In scope:
- `POST /api/v1/merchant-service/work-orders` creates repair/complaint/rectification/service work orders.
- `GET /api/v1/merchant-service/work-orders` lists work orders.
- `POST /api/v1/merchant-service/work-orders/{id}/assign` assigns owner department/person and changes status to `assigned`.
- `POST /api/v1/merchant-service/work-orders/{id}/actions` appends processing notes and optionally changes status.
- `POST /api/v1/merchant-service/work-orders/{id}/close` closes a work order with resolution, satisfaction score, and evidence references.
- Every write uses an idempotency key and audit event.
- Permissions are inserted through Flyway and checked by Controller.

Out of scope:
- Merchant bill portal, collection reminders, fee disputes, rent/CAM calculation, and AR aging views.
- Third-party customer service SDK sync.
- File storage or attachment upload; this slice stores evidence attachment ID references only.

## Files

- Create: `backend/src/main/java/com/complex/platform/merchantservice/MerchantServiceController.java`
- Create: `backend/src/main/java/com/complex/platform/merchantservice/MerchantServiceService.java`
- Create: `backend/src/main/java/com/complex/platform/merchantservice/MerchantServiceRepository.java`
- Create: `backend/src/main/java/com/complex/platform/merchantservice/JdbcMerchantServiceRepository.java`
- Create: `backend/src/main/java/com/complex/platform/merchantservice/MerchantWorkOrderRecord.java`
- Create: `backend/src/main/java/com/complex/platform/merchantservice/MerchantWorkOrderActionRecord.java`
- Create: `backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderCreateRequest.java`
- Create: `backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderAssignRequest.java`
- Create: `backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderActionRequest.java`
- Create: `backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderCloseRequest.java`
- Create: `backend/src/main/resources/db/migration/V022__p1_merchant_service_work_orders.sql`
- Create: `backend/src/test/java/com/complex/platform/merchantservice/MerchantServiceServiceTests.java`
- Create: `backend/src/test/java/com/complex/platform/merchantservice/MerchantServiceControllerTests.java`
- Create: `backend/src/test/java/com/complex/platform/merchantservice/MerchantServiceJdbcIntegrationTests.java`
- Create: `docs/p1-merchant-service-work-order-api-contract-v0.1.md`
- Modify: JDBC integration tests that assert Flyway version count, if any fixed expected version is present.

## Data Structure

`merchant_service_work_orders`:
- `id varchar(64) primary key`
- `merchant_id varchar(64) not null`
- `space_id varchar(64)`
- `contract_id varchar(64)`
- `work_order_type varchar(32) not null`: `repair`, `complaint`, `rectification`, `service`
- `priority varchar(16) not null`: `low`, `medium`, `high`, `critical`
- `status varchar(32) not null`: `created`, `assigned`, `processing`, `pending_merchant`, `resolved`, `closed`, `cancelled`
- `title varchar(160) not null`
- `description varchar(2000) not null`
- `reported_by_user_id varchar(64) not null`
- `reported_channel varchar(64) not null`
- `owner_department varchar(64)`
- `owner_user_id varchar(64)`
- `due_at timestamptz`
- `closed_at timestamptz`
- `resolution varchar(2000)`
- `satisfaction_score integer`
- `evidence_attachment_ids jsonb not null default '[]'`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

`merchant_service_work_order_actions`:
- `id varchar(64) primary key`
- `work_order_id varchar(64) not null references merchant_service_work_orders(id)`
- `action_type varchar(32) not null`: `created`, `assigned`, `commented`, `status_changed`, `closed`
- `from_status varchar(32)`
- `to_status varchar(32)`
- `operator_user_id varchar(64) not null`
- `operator_note varchar(1000) not null`
- `evidence_attachment_ids jsonb not null default '[]'`
- `created_at timestamptz not null`

## API/Permission/Event Contract

- `GET /api/v1/merchant-service/work-orders`
  - Permission: `merchant.service_work_order.view`
  - Event: `merchant_service.work_order.viewed`
- `POST /api/v1/merchant-service/work-orders`
  - Permission: `merchant.service_work_order.create`
  - Success event: `merchant_service.work_order.created`
  - Failure event: `merchant_service.work_order.create_failed`
- `POST /api/v1/merchant-service/work-orders/{id}/assign`
  - Permission: `merchant.service_work_order.assign`
  - Success event: `merchant_service.work_order.assigned`
  - Failure event: `merchant_service.work_order.assign_failed`
- `POST /api/v1/merchant-service/work-orders/{id}/actions`
  - Permission: `merchant.service_work_order.process`
  - Success event: `merchant_service.work_order.action_added`
  - Failure event: `merchant_service.work_order.action_failed`
- `POST /api/v1/merchant-service/work-orders/{id}/close`
  - Permission: `merchant.service_work_order.close`
  - Success event: `merchant_service.work_order.closed`
  - Failure event: `merchant_service.work_order.close_failed`

## TDD Tasks

### Task 1: Service Behavior

**Files:**
- Create: `backend/src/test/java/com/complex/platform/merchantservice/MerchantServiceServiceTests.java`
- Create/modify implementation files listed above.

- [ ] **Step 1: Write failing service tests**

Add tests for:
- Create validates merchant ID, type, priority, title, description, reporter, channel.
- Create stores first action `created` and audit event.
- Assign requires existing work order, owner department, owner user, and moves status to `assigned`.
- Action rejects updates to `closed` work orders.
- Close requires resolution and rejects invalid satisfaction score.
- Duplicate idempotency key with same request returns first response; with different request returns `IDEMPOTENCY_CONFLICT`.

- [ ] **Step 2: Run red test**

Run on 160:
`cd /home/james/complex-management/backend && mvn -Dtest=MerchantServiceServiceTests test`

Expected: FAIL because `merchantservice` classes do not exist.

- [ ] **Step 3: Implement minimal service/domain/repository interface**

Add records, DTOs, service validation, in-memory-testable repository contract, audit events, and idempotency integration.

- [ ] **Step 4: Run green service test**

Run on 160:
`cd /home/james/complex-management/backend && mvn -Dtest=MerchantServiceServiceTests test`

Expected: PASS.

### Task 2: Controller Contract

**Files:**
- Create: `backend/src/main/java/com/complex/platform/merchantservice/MerchantServiceController.java`
- Create: `backend/src/test/java/com/complex/platform/merchantservice/MerchantServiceControllerTests.java`

- [ ] **Step 1: Write failing controller tests**

Add MockMvc tests for:
- Unauthenticated list is 401 or 403.
- Create without idempotency key returns `VALIDATION_FAILED`.
- Create requires `merchant.service_work_order.create`.
- Assign requires `merchant.service_work_order.assign`.
- Close returns response body with status `closed`.

- [ ] **Step 2: Run red test**

Run:
`cd /home/james/complex-management/backend && mvn -Dtest=MerchantServiceControllerTests test`

Expected: FAIL because Controller does not exist.

- [ ] **Step 3: Implement Controller**

Use existing API response shape and controller idempotency validation.

- [ ] **Step 4: Run green controller test**

Run:
`cd /home/james/complex-management/backend && mvn -Dtest=MerchantServiceControllerTests test`

Expected: PASS.

### Task 3: JDBC Persistence And Permissions

**Files:**
- Create: `backend/src/main/resources/db/migration/V022__p1_merchant_service_work_orders.sql`
- Create: `backend/src/main/java/com/complex/platform/merchantservice/JdbcMerchantServiceRepository.java`
- Create: `backend/src/test/java/com/complex/platform/merchantservice/MerchantServiceJdbcIntegrationTests.java`

- [ ] **Step 1: Write failing JDBC integration test**

Verify:
- Flyway applies V022.
- Work order and actions persist and can be listed.
- Permissions are present in `permissions` and assigned to `role_platform_admin`.

- [ ] **Step 2: Run red test**

Run:
`cd /home/james/complex-management/backend && mvn -Dtest=MerchantServiceJdbcIntegrationTests test`

Expected: FAIL because migration/repository are missing.

- [ ] **Step 3: Implement migration and JDBC repository**

Use `jsonb` for evidence ID arrays and `ON CONFLICT` only where idempotency guarantees make the write safe.

- [ ] **Step 4: Run green integration test**

Run:
`cd /home/james/complex-management/backend && mvn -Dtest=MerchantServiceServiceTests,MerchantServiceControllerTests,MerchantServiceJdbcIntegrationTests test`

Expected: PASS.

### Task 4: Documentation And Regression Gates

**Files:**
- Create: `docs/p1-merchant-service-work-order-api-contract-v0.1.md`

- [ ] **Step 1: Document API contract**

Document scope, endpoints, permissions, event names, status machine, validation rules, anti-patterns, test cases, error handling, and references.

- [ ] **Step 2: Run focused verification**

Run:
`cd /home/james/complex-management/backend && mvn -Dtest=MerchantServiceServiceTests,MerchantServiceControllerTests,MerchantServiceJdbcIntegrationTests test`

- [ ] **Step 3: Run full backend regression**

Run:
`cd /home/james/complex-management/backend && mvn test`

- [ ] **Step 4: Run P0 contract gate**

Run:
`cd /home/james/complex-management && npm run verify:p0-contract`

Expected: PASS; P1 addition must not drift P0 contract.

- [ ] **Step 5: Run focused security scan**

Run:
`cd /home/james/complex-management && /home/james/bin/semgrep scan --metrics=off --severity WARNING --severity ERROR --config p/java --config p/findsecbugs --config p/secrets backend/src/main/java/com/complex/platform/merchantservice backend/src/main/resources/db/migration/V022__p1_merchant_service_work_orders.sql`

Expected: no confirmed high-risk finding.

## Acceptance Criteria

- A merchant service work order can be created for a merchant with type, priority, title, description, reporter, channel, due time, and evidence references.
- A work order can be assigned to an owner department and owner user, and an assignment action is recorded.
- A processing action can add notes/evidence and move status to `processing`, `pending_merchant`, or `resolved`.
- A work order can be closed only with a resolution; satisfaction score must be 1-5 when present.
- Closed or cancelled work orders cannot be reassigned or processed.
- Every write is idempotent and writes success/failure audit events.
- Controller enforces operation-level permissions.
- Flyway migration creates tables, indexes, checks, permissions, and platform-admin role permissions.
- Full backend tests and P0 contract verification pass.

## Risks And Rollback

- Risk: This P1 API is not in P0 OpenAPI, so generated clients will not see it yet. Mitigation: document P1 contract separately and keep P0 verification unchanged.
- Risk: Status machine may later need SLA escalation or merchant-side reopen. Mitigation: store action ledger and keep `pending_merchant`/`resolved` states for extension.
- Risk: Evidence attachment IDs can reference missing attachments until cross-module FK is introduced. Mitigation: document that this slice stores references only and relies on evidence service validation in later P1/P2.
- Rollback: remove V022 from non-applied environments, remove `merchantservice` package and tests, and remove the P1 API contract document. In an applied database, use a forward migration to disable permissions and archive tables instead of destructive rollback.

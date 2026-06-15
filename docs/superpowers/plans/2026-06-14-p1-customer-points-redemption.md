# P1 Customer Points Redemption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the P1 customer points detail and redemption backend loop for App/Miniapp without changing the P0 customer OpenAPI contract.

**Architecture:** Extend the existing `customer` Spring package. `customer_points_accounts` remains the balance authority; new P1 tables store immutable points ledger rows, redemption catalog items, and customer redemption records. Successful redemption deducts points and calls the inventory service to deduct gift stock with the same idempotency boundary.

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL, Flyway, JUnit 6, MockMvc, Semgrep on 192.168.88.160.

---

## Scope

In scope:
- Customer token scoped points ledger query.
- Active redemption catalog query.
- Customer redemption creation with idempotency.
- Points balance deduction and ledger row creation.
- Inventory stock deduction through existing inventory service.
- Redemption record query.
- P1 API contract document and Flyway migration.

Out of scope:
- Admin UI for catalog maintenance.
- Gift pickup verification and write-off.
- External supplier or warehouse device SDK.
- P0 OpenAPI changes.

## Files

- Modify: `backend/src/main/java/com/complex/platform/customer/CustomerService.java`
- Modify: `backend/src/main/java/com/complex/platform/customer/CustomerController.java`
- Modify: `backend/src/main/java/com/complex/platform/customer/CustomerRepository.java`
- Modify: `backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java`
- Modify: `backend/src/test/java/com/complex/platform/customer/CustomerServiceTests.java`
- Modify: `backend/src/test/java/com/complex/platform/customer/CustomerControllerTests.java`
- Modify: `backend/src/test/java/com/complex/platform/customer/CustomerJdbcIntegrationTests.java`
- Create: `backend/src/main/java/com/complex/platform/customer/CustomerPointsLedgerRecord.java`
- Create: `backend/src/main/java/com/complex/platform/customer/CustomerRedemptionCatalogItemRecord.java`
- Create: `backend/src/main/java/com/complex/platform/customer/CustomerPointsRedemptionRecord.java`
- Create: `backend/src/main/java/com/complex/platform/customer/dto/CustomerPointsRedemptionRequest.java`
- Create: `backend/src/main/resources/db/migration/V025__p1_customer_points_redemption.sql`
- Create: `docs/p1-customer-points-redemption-api-contract-v0.1.md`

## Tasks

### Task 1: Service behavior

- [x] Add failing service tests for ledger query, catalog query, successful redemption, insufficient points, and inventory failure.
- [x] Run `mvn -Dtest=CustomerServiceTests test` on 160 and confirm compile/test failure caused by missing P1 records and service methods.
- [x] Add records, DTO, repository methods, and minimal service implementation.
- [x] Run `mvn -Dtest=CustomerServiceTests test` on 160 and confirm pass.

### Task 2: HTTP controller behavior

- [x] Add failing controller tests for `GET /api/v1/customer/points/ledger`, `GET /api/v1/customer/points/redemption-items`, `POST /api/v1/customer/points/redemptions`, and `GET /api/v1/customer/points/redemptions`.
- [x] Run `mvn -Dtest=CustomerControllerTests test` on 160 and confirm failure caused by missing routes.
- [x] Add controller routes using customer token auth only.
- [x] Run `mvn -Dtest=CustomerControllerTests test` on 160 and confirm pass.

### Task 3: PostgreSQL persistence

- [x] Add failing JDBC integration tests for migration V025, catalog listing, ledger insert/list, redemption insert/list, and balance update.
- [x] Run `mvn -Dtest=CustomerJdbcIntegrationTests test` on 160 and confirm migration/version failure or missing repository method failure.
- [x] Add Flyway migration and JDBC repository implementation.
- [x] Update migration version assertions from `024` to `025`.
- [x] Run `mvn -Dtest=CustomerJdbcIntegrationTests test` on 160 and confirm pass.

### Task 4: Documentation and gates

- [x] Add the P1 API contract document.
- [x] Sync all modified files to 160.
- [x] Run `mvn -Dtest=CustomerServiceTests,CustomerControllerTests,CustomerJdbcIntegrationTests test`.
- [x] Run `npm run verify:p0-contract`.
- [x] Run focused Semgrep on new/modified customer files and V025.
- [x] Run full `mvn test`.

## Acceptance Criteria

- A customer can query points ledger rows scoped to their own token.
- A customer can query only active redemption items.
- A customer can redeem an active item only when points balance and inventory stock are sufficient.
- A successful redemption deducts points, creates a negative ledger row, persists a redemption record, calls inventory stock deduction, and emits audit events.
- If inventory deduction fails, no points are deducted and no redemption record is created.
- Idempotent retry with the same key returns the same redemption response.
- P0 OpenAPI contract verification remains green.

## Risks And Rollback

- Risk: inventory deduction behavior may evolve later. Mitigation: call the existing inventory service with a clear source type and item/location from catalog.
- Risk: P1 endpoints are not in generated clients yet. Mitigation: keep a separate P1 contract document and do not mutate P0 OpenAPI.
- Rollback: remove V025 before deployment. If already applied, use a forward migration to disable permissions/routes and archive P1 tables instead of destructive rollback.

# P0 Ticketing Customer Order Credential Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the P0 ticketing and customer-facing M2 loop: price rules, ticket orders, pay intents, issuance, dynamic credentials, refunds, customer login, ticket purchase, ticket QR, consent, points summary, and customer order reads.

**Architecture:** Extend the existing Java 21 + Spring Boot 4.x modular monolith. Keep formal OpenAPI paths under `/api/v1/ticketing/*` and `/api/v1/customer/*`, use PostgreSQL + Flyway persistence, exact permission checks, idempotency service, and audit event writes for every write/read operation.

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL, Flyway, NamedParameterJdbcTemplate, Maven, JUnit 6, MockMvc.

---

## Scope

Implement the remaining P0 ticketing/customer endpoints from `docs/openapi/openapi-p0.json` that are not covered by the current `TicketingController`:

- `GET /api/v1/ticketing/issued-entitlements`
- `POST /api/v1/ticketing/orders`
- `POST /api/v1/ticketing/orders/{order_id}/issue`
- `POST /api/v1/ticketing/orders/{order_id}/pay-intents`
- `GET /api/v1/ticketing/price-rules`
- `POST /api/v1/ticketing/price-rules`
- `POST /api/v1/ticketing/refunds`
- `POST /api/v1/ticketing/refunds/{refund_id}/approve`
- `POST /api/v1/ticketing/ticket-types/{id}/publish`
- `GET /api/v1/ticketing/tickets/{ticket_id}/credential`
- `POST /api/v1/customer/auth/phone-login`
- `GET /api/v1/customer/ticket-products`
- `POST /api/v1/customer/ticket-orders`
- `GET /api/v1/customer/orders`
- `GET /api/v1/customer/tickets/{ticket_id}/qr`
- `POST /api/v1/customer/consents`
- `GET /api/v1/customer/points`

Customer bar reservation endpoints may be implemented in this slice if they can be done without dragging in the full bar table/order model. If the bar model is required, add only customer DTO/controller placeholders backed by formal persistence and leave table availability data for the bar slice.

## File Structure

Create:

- `backend/src/main/resources/db/migration/V009__p0_ticketing_customer_orders_credentials.sql`
- `backend/src/main/java/com/complex/platform/customer/CustomerController.java`
- `backend/src/main/java/com/complex/platform/customer/CustomerService.java`
- `backend/src/main/java/com/complex/platform/customer/CustomerRepository.java`
- `backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java`
- `backend/src/main/java/com/complex/platform/customer/dto/*.java`
- Customer records/responses under `backend/src/main/java/com/complex/platform/customer/`
- Ticket order, price rule, credential, refund records/responses under `backend/src/main/java/com/complex/platform/ticketing/`
- New DTOs under `backend/src/main/java/com/complex/platform/ticketing/dto/`
- `backend/src/test/java/com/complex/platform/customer/CustomerServiceTests.java`
- `backend/src/test/java/com/complex/platform/customer/CustomerControllerTests.java`
- `backend/src/test/java/com/complex/platform/customer/CustomerJdbcIntegrationTests.java`

Modify:

- `backend/src/main/java/com/complex/platform/ticketing/TicketingController.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketingService.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java`
- `backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingControllerTests.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingJdbcIntegrationTests.java`
- `docs/openapi/openapi-p0.yaml` and `docs/openapi/openapi-p0.json` only if examples/minimums must be corrected to match already-approved P0 rules.

Do not create or restore `apps/api`. Do not introduce TypeScript/SQLite/Fastify runtime code.

## Data Structures

Add Flyway tables with integer cents and CNY-only defaults:

- `ticket_price_rules`: ticket type, variant type, price mode, fixed price, multiplier basis or derived metadata, channel, status.
- `ticket_orders`: order number, customer id, ticket type, quantity, visit date, sales channel, amount, pay status, issue status, status.
- `ticket_order_items`: issued ticket/order item rows linked to ticket order and ticket type.
- `ticket_credentials`: ticket/order/entitlement credential token, nonce, signature/hash, expires at, status, refreshed count.
- `ticket_refunds`: target order/ticket, refund amount, reason, refund direction/channel, status, approved fields.
- `customers`: phone, masked phone, status, last login.
- `customer_sessions`: P0 lightweight customer token/session records if current auth module has no customer-token store.
- `customer_consents`: consent type, accepted, version, channel, accepted at, withdrawn at.
- `customer_points_accounts`: customer id, balance, level, recent summary json.
- Optional `customer_bar_reservations` only if lightweight customer reservation endpoints are included.

Seed required permissions for the new operation permission codes if missing. `role_platform_admin` must receive them for 160 smoke/dev tests.

## TDD Tasks

### Task 1: Ticket Price Rules

- [ ] Write failing service tests proving: create fixed price rule, create derived rule, reject unknown ticket type, idempotent create returns one row, list writes `ticket_price_variant.viewed`.
- [ ] Run only the new/changed ticketing service tests and confirm they fail for missing behavior.
- [ ] Implement `TicketPriceRuleRequest`, record/response types, repository methods, service methods, controller routes, permission checks, idempotency and audit events.
- [ ] Add JDBC integration test proving rows persist through Flyway V009.
- [ ] Run targeted tests and keep them green.

Required events:

- Create success/failure: `ticket_price_variant.changed` / `ticket_price_variant.change_failed`
- List success/failure: `ticket_price_variant.viewed` / `ticket_price_variant.view_failed`

### Task 2: Ticket Orders, Pay Intents, Issue From Order

- [ ] Write failing service tests proving: order amount is computed from price rule or ticket type base price, quantity must be positive, pay intent is created with `created` status, issuing an order creates entitlements once, duplicate issue is idempotent.
- [ ] Run the tests and confirm RED.
- [ ] Add `TicketOrderRequest`, order records/responses, repository save/list/find/update methods, and controller routes.
- [ ] Implement `POST /api/v1/ticketing/orders`, `POST /api/v1/ticketing/orders/{order_id}/pay-intents`, and `POST /api/v1/ticketing/orders/{order_id}/issue`.
- [ ] Reuse the existing entitlement issuance logic, but bind issued entitlements to the order id and customer when available.
- [ ] Run targeted service/controller/JDBC tests.

Required permissions/events:

- Order create: `ticket.order.create`, `ticket_order.created` / `ticket_order.create_failed`
- Pay intent: `finance.payment_intent.create`, `payment.intent_created` / `payment.intent_failed`
- Issue: `ticket.ticket.issue`, `ticket.issued` / `ticket.issue_failed`

### Task 3: Publish, Issued Entitlement List, Dynamic Credential

- [ ] Write failing tests proving: unpublished ticket type cannot be ordered if status requires publish, publish changes status, issued entitlements are listable, credential refresh returns short-lived token, expired/unknown credential cannot verify.
- [ ] Run tests and confirm RED.
- [ ] Implement `publishTicketType`, `listIssuedEntitlements`, and `getTicketCredential`.
- [ ] Update verification lookup so credential payload can resolve via active `ticket_credentials` first, then legacy entitlement code fallback.
- [ ] Run ticketing targeted tests and Android terminal smoke-compatible tests.

Required permissions/events:

- Publish: `ticket.product.manage`, `ticket_price.published` / `ticket_price.publish_failed`
- Issued entitlements list: `ticket.verify.view`, `ticket_issued_entitlement.viewed` / `ticket_issued_entitlement.viewed_failed`
- Credential: `ticket.credential.view`, `ticket_credential.refreshed` / `ticket_credential.refresh_failed`

### Task 4: Ticket Refund Request And Approval

- [ ] Write failing tests proving: refund request records target and amount, refund amount cannot exceed order amount, approving refund changes status and does not delete issued/verification history.
- [ ] Run tests and confirm RED.
- [ ] Implement refund records, DTOs, repository methods, service methods, and controller routes.
- [ ] If an approved refund affects active unused entitlements, mark the refund record and leave entitlement reversal as traceable status change; do not hard delete.
- [ ] Run targeted tests.

Required permissions/events:

- Request: `ticket.refund.request`, `ticket_refund.requested` / `ticket_refund.request_failed`
- Approve: `ticket.refund.approve`, `ticket_refund.approved` / `ticket_refund.approve_failed`

### Task 5: Customer Auth, Ticket Products, Ticket Orders, QR, Consent, Points

- [ ] Write failing customer service/controller tests proving: phone login returns a customer token, ticket products are public listable, customer order creates ticket order under customer id, customer orders list only that customer, customer QR delegates to ticket credential, consent records accepted/withdrawn state, points summary returns balance/level/recent summary.
- [ ] Run tests and confirm RED.
- [ ] Implement customer package with DTOs, records, repository, service and controller.
- [ ] Use explicit authentication boundary: public endpoints do not require staff permission; customer-owned endpoints must use customer token/header logic or a P0 deterministic dev token accepted by tests and smoke.
- [ ] Run customer targeted tests.

Required events:

- `customer.auth.logged_in` / `customer.auth.login_failed`
- `customer.ticket_product.viewed` / `customer.ticket_product.view_failed`
- `customer.ticket_order.created` / `customer.ticket_order.create_failed`
- `customer.order.viewed` / `customer.order.view_failed`
- `customer.ticket_qr.refreshed` / `customer.ticket_qr.refresh_failed`
- `customer.consent.changed` / `customer.consent.change_failed`
- `customer.points.viewed` / `customer.points.view_failed`

### Task 6: Contract, Regression, Security Review

- [ ] Run `npm run verify:p0-contract`.
- [ ] Run targeted backend tests:

```bash
cd /home/james/complex-management/backend && mvn -q -Dtest=TicketingServiceTests,TicketingControllerTests,TicketingJdbcIntegrationTests,CustomerServiceTests,CustomerControllerTests,CustomerJdbcIntegrationTests test
```

- [ ] Run full P0 verification:

```bash
/opt/complex/scripts/complex-verify-p0.sh
```

- [ ] Review changed code for SQL injection, missing permission checks, missing idempotency, missing audit events, over-broad customer order access, and credential replay/expiry gaps.
- [ ] Confirm Flyway latest version is `009`.

## Acceptance Criteria

- All listed ticketing/customer OpenAPI paths have formal Java Controller + DTO + Service + Repository implementation.
- All write operations require idempotency unless OpenAPI marks them public/non-idempotent.
- Staff endpoints call exact `AuthorizationService.requirePermission(...)` with OpenAPI permission codes.
- Customer endpoints do not accidentally require staff permissions; they must have an explicit customer identity boundary.
- Every success and failure path records the exact OpenAPI audit event names.
- Dynamic QR credential is short-lived, not a bare reusable entitlement code, and ticket verification can consume it.
- No amount is stored as floating point; all money is integer cents and currency remains `CNY`.
- PostgreSQL/Flyway migrations are repeatable and integration tests prove persistence.
- `verify:p0-contract` and `/opt/complex/scripts/complex-verify-p0.sh` pass on 160.

## Risks And Rollback

- Credential changes can break Android terminal smoke. Roll back by restoring the pre-slice tar snapshot and keeping entitlement-code fallback until Android serialization is updated.
- Customer token handling can become accidental auth bypass. Keep P0 dev token logic explicit, tested, and isolated in customer service/controller only.
- Ticket order issuance can double-create entitlements. Use idempotency operation id containing the order id and repository uniqueness on order issue where possible.
- Refund approval can corrupt historical verification. Never delete issued entitlement or verification rows; only add status/reversal/refund records.

Rollback command pattern:

```bash
tar -xzf /home/james/complex-management-backups/<snapshot>.tgz -C /home/james/complex-management --strip-components=1
/opt/complex/scripts/complex-verify-p0.sh
```

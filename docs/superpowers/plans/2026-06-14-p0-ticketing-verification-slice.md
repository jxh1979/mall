# P0 Ticketing Entitlement Verification Slice Plan

## 1. Objective

Move the current ticketing, entitlement, terminal offline package, verification preview, verification execution, and terminal sync smoke-only flow into formal Java 21 + Spring Boot 4.x + PostgreSQL + Flyway code on 192.168.88.160.

This slice must preserve the existing Android P0 smoke flow while removing the matching temporary ticketing handlers from `P0SmokeController`.

## 2. In Scope

Formalize these OpenAPI P0 paths:

- `GET /api/v1/ticketing/ticket-types`
- `POST /api/v1/ticketing/ticket-types`
- `GET /api/v1/ticketing/entitlement-templates`
- `POST /api/v1/ticketing/entitlement-templates`
- `POST /api/v1/ticketing/ticket-types/{id}/entitlements`
- `POST /api/v1/ticketing/ticket-types/{id}/issue`
- `GET /api/v1/terminals/offline-entitlements`
- `POST /api/v1/ticketing/verifications/preview`
- `POST /api/v1/ticketing/verifications`
- `GET /api/v1/ticketing/verifications`
- `POST /api/v1/terminals/sync-tasks`

## 3. Out of Scope

- Full ticket order lifecycle beyond the direct issue endpoint.
- Aggregated payment/refund implementation.
- Dynamic QR anti-replay cryptographic credential service.
- Complete pricing calendar and derived price rules.
- Full terminal device binding and return formalization; keep remaining smoke handlers until their own slice.

## 4. Files To Add

- `backend/src/main/resources/db/migration/V006__p0_ticketing_verification.sql`
- `backend/src/main/java/com/complex/platform/ticketing/TicketingController.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketingService.java`
- `backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java`
- `backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java`
- `backend/src/main/java/com/complex/platform/ticketing/*Record.java`
- `backend/src/main/java/com/complex/platform/ticketing/dto/*Request.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingControllerTests.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java`
- `backend/src/test/java/com/complex/platform/ticketing/TicketingJdbcIntegrationTests.java`

## 5. Files To Modify

- `backend/src/main/java/com/complex/platform/smoke/P0SmokeController.java`
  - Remove only ticketing/offline entitlement/verification/sync handlers covered by this slice.
  - Keep device bind, evidence event, and device return handlers until their formal slices.
- `apps/android-terminal/src/main/kotlin/com/complex/terminal/P0Fixture.kt`
  - Align ticketing fields to OpenAPI snake_case: `ticket_name`, `base_calendar_price_mode`, `weekday_price_cent`, `entitlement_type`, `default_total_times`, `default_value_cent`, `entitlement_template_id`, `value_amount_cent`, `order_id`.
- `apps/android-terminal/src/test/kotlin/com/complex/terminal/P0FixtureTest.kt`
  - Assert ticketing fixture payloads use OpenAPI field names and still include `idempotency_key` and `operator_note`.

## 6. Data Model

Add PostgreSQL tables with idempotent Flyway DDL:

- `ticket_types`
  - `id`, `ticket_name`, `base_calendar_price_mode`, `weekday_price_cent`, `weekend_price_cent`, `holiday_price_cent`, `stock_required`, `status`, `created_at`, `updated_at`
- `entitlement_templates`
  - `id`, `name`, `entitlement_type`, `default_total_times`, `default_value_cent`, `status`, `created_at`, `updated_at`
- `ticket_type_entitlements`
  - `id`, `ticket_type_id`, `entitlement_template_id`, `quantity`, `total_times`, `value_amount_cent`, `effective_rule`, `created_at`, `updated_at`
- `issued_entitlements`
  - `id`, `ticket_type_id`, `order_id`, `entitlement_template_id`, `entitlement_code`, `entitlement_type`, `merchant_id`, `venue_id`, `point_id`, `total_times`, `used_times`, `value_amount_cent`, `status`, `valid_from`, `valid_to`, `created_at`, `updated_at`
- `ticket_verifications`
  - `id`, `verification_no`, `entitlement_id`, `entitlement_code`, `credential_type`, `credential_payload`, `device_id`, `point_id`, `status`, `result_reason`, `source_type`, `client_task_id`, `created_at`
- `terminal_sync_tasks`
  - `id`, `device_code`, `staff_id`, `client_task_id`, `task_type`, `status`, `reason`, `verification_id`, `verification_no`, `occurred_at`, `payload`, `created_at`

Seed permissions into `permissions` and attach to `role_platform_admin`:

- `ticket.product.view`
- `ticket.product.manage`
- `ticket.entitlement.view`
- `ticket.entitlement.manage`
- `ticket.order.create`
- `verification.execute`
- `verification.view`
- `terminal.device.bind`

## 7. Interface Rules

- All formal endpoints must call `AuthorizationService.requirePermission(...)` with the exact OpenAPI permission.
- All write endpoints with `x-idempotency-required: true` must reject missing/blank `idempotency_key`.
- All SQL must use parameterized `NamedParameterJdbcTemplate`; no user input concatenation.
- Response bodies may keep the existing P0 Android-compatible shape where OpenAPI only references generic `Success` or `Page`, but field names must serialize as snake_case through the existing Jackson strategy.
- Direct issue endpoint may accept `order_id` as an external/order reference for this slice and issue entitlements from bound templates. Full ticket order validation remains out of scope.

## 8. Audit Events

Use exact OpenAPI event names:

- `ticket_product.viewed` / `ticket_product.view_failed`
- `ticket_product.changed` / `ticket_product.change_failed`
- `ticket_entitlement_template.viewed` / `ticket_entitlement_template.viewed_failed`
- `ticket_entitlement_template.changed` / `ticket_entitlement_template.changed_failed`
- `ticket_product_entitlement.bound` / `ticket_product_entitlement.bound_failed`
- `ticket_order.issued` / `ticket_order.issued_failed`
- `terminal.offline_entitlements.viewed` / `terminal.offline_entitlements.viewed_failed`
- `verification.previewed` / `verification.failed`
- `verification.succeeded` / `verification.failed`
- `verification.viewed` / `verification.view_failed`
- `terminal.sync_task.accepted` / `terminal.sync_task.accepted_failed`

## 9. TDD Order

1. Add failing controller tests for missing authentication/permission and missing idempotency key on ticket type creation and verification.
2. Add failing service tests for:
   - ticket type creation writes `ticket_product.changed`
   - entitlement template creation is idempotent
   - direct issue creates an entitlement code
   - preview does not consume remaining times
   - verification consumes exactly one remaining time and is idempotent
   - sync task accepts offline verification and rejects duplicate consumption through idempotency
3. Add failing JDBC integration tests for persistence, list endpoints, and Flyway permission seeds.
4. Implement minimal service/repository/controller.
5. Update Android fixture and tests.
6. Remove replaced smoke handlers.

## 10. Verification

Run, in order:

- `cd /home/james/complex-management/backend && mvn -q -Dtest=TicketingServiceTests,TicketingControllerTests,TicketingJdbcIntegrationTests test`
- `/opt/complex/scripts/complex-verify-p0.sh`

Expected result:

- Backend tests pass.
- `verify:p0-contract` passes.
- Android P0 smoke still passes, proving the terminal flow uses formal ticketing endpoints.
- P0 verification finishes with `P0 verification PASS`.

## 11. Risk And Rollback

Risk:

- Android fixture may still send old camelCase ticketing fields.
- Existing smoke handlers may shadow formal controllers if not removed.
- Idempotency operation ID choice may accidentally prevent legitimate repeated verifications with different payloads.

Rollback:

- Create a tar snapshot of `/home/james/complex-management` before editing.
- If full P0 verification fails and cannot be fixed quickly, restore the snapshot and redeploy the previous release.

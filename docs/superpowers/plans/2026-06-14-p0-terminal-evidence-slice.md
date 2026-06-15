# P0 Terminal Device And Evidence Slice Plan

## 1. Objective

Move the remaining Android-terminal-dependent device binding, device return, device unbind, and evidence event reporting paths from `P0SmokeController` into formal Java 21 + Spring Boot 4.x + PostgreSQL + Flyway code on 192.168.88.160.

This slice closes the current terminal smoke dependency on in-memory device/evidence handlers while preserving the Android P0 smoke workflow.

## 2. In Scope

Formalize these OpenAPI P0 paths:

- `POST /api/v1/terminals/devices/bind`
- `POST /api/v1/terminals/devices/return`
- `POST /api/v1/terminals/devices/{id}/unbind`
- `POST /api/v1/evidence/events`
- `GET /api/v1/evidence/events`

## 3. Out of Scope

- Real MDM / Android Management API integration.
- Device vendor SDK integration.
- Video platform or object-storage evidence binding.
- Advanced device inventory depreciation, repair, and procurement flows.
- Full alerting rules based on terminal events.

## 4. Files To Add

- `backend/src/main/resources/db/migration/V007__p0_terminal_evidence.sql`
- `backend/src/main/java/com/complex/platform/terminal/TerminalDeviceController.java`
- `backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java`
- `backend/src/main/java/com/complex/platform/terminal/TerminalDeviceRepository.java`
- `backend/src/main/java/com/complex/platform/terminal/JdbcTerminalDeviceRepository.java`
- `backend/src/main/java/com/complex/platform/terminal/*Record.java`
- `backend/src/main/java/com/complex/platform/terminal/dto/*Request.java`
- `backend/src/main/java/com/complex/platform/evidence/EvidenceEventController.java`
- `backend/src/main/java/com/complex/platform/evidence/EvidenceEventService.java`
- `backend/src/main/java/com/complex/platform/evidence/EvidenceEventRepository.java`
- `backend/src/main/java/com/complex/platform/evidence/JdbcEvidenceEventRepository.java`
- `backend/src/main/java/com/complex/platform/evidence/*Record.java`
- `backend/src/main/java/com/complex/platform/evidence/dto/*Request.java`
- `backend/src/test/java/com/complex/platform/terminal/TerminalDeviceControllerTests.java`
- `backend/src/test/java/com/complex/platform/terminal/TerminalDeviceServiceTests.java`
- `backend/src/test/java/com/complex/platform/terminal/TerminalDeviceJdbcIntegrationTests.java`
- `backend/src/test/java/com/complex/platform/evidence/EvidenceEventControllerTests.java`
- `backend/src/test/java/com/complex/platform/evidence/EvidenceEventServiceTests.java`
- `backend/src/test/java/com/complex/platform/evidence/EvidenceEventJdbcIntegrationTests.java`

## 5. Files To Modify

- `backend/src/main/java/com/complex/platform/smoke/P0SmokeController.java`
  - Remove device bind, device return, evidence event POST, and evidence event GET handlers.
  - Keep the class only if any unrelated temporary P0 smoke handlers remain; otherwise delete it.
- `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt`
  - Align request serialization to OpenAPI snake_case using `@SerialName`.
  - Device bind request must send `device_id`, `terminal_type`, `binding_scope`, `bound_space_id`, optional `mdm_device_ref`, optional `kiosk_mode`, and `idempotency_key`.
  - Device return request must send `device_code`, `has_pending_tasks`, `staff_id`, `reason`, and `idempotency_key`.
  - Event batch request must send `device_code`, `staff_id`, `events`, and `idempotency_key`.
  - Terminal event item must send `event_type`, `event_category`, `result`, `summary`, `payload`, `occurred_at`.
- `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalWorkflow.kt`
  - Generate stable idempotency keys for bind, event report, return, and unbind-compatible return actions.
  - Map existing device code to OpenAPI `device_id` while keeping the response shape Android uses.
- `apps/android-terminal/src/test/kotlin/com/complex/terminal/*`
  - Add or update tests proving Android terminal request payloads use OpenAPI field names.

## 6. Data Model

Add PostgreSQL tables with idempotent Flyway DDL:

- `terminal_devices`
  - `id`, `device_id`, `device_code`, `terminal_type`, `binding_scope`, `bound_space_id`, `mdm_device_ref`, `kiosk_mode`, `merchant_id`, `venue_id`, `area_id`, `location_point_id`, `staff_id`, `status`, `bound_at`, `returned_at`, `unbound_at`, `return_reason`, `unbind_reason`, `created_at`, `updated_at`
  - Unique constraints: `device_id`, `device_code`
- `terminal_device_status_events`
  - `id`, `device_id`, `device_code`, `event_type`, `status`, `reason`, `staff_id`, `created_at`
- `evidence_events`
  - `id`, `device_code`, `staff_id`, `event_type`, `event_category`, `result`, `summary`, `payload`, `occurred_at`, `received_at`

Seed permissions into `permissions` and attach to `role_platform_admin`:

- `terminal.device.bind`
- `terminal.device.unbind`
- `evidence.event.create`
- `evidence.event.view`

`terminal.device.bind` may already exist from V006; use `ON CONFLICT` and do not break prior migration.

## 7. Interface Rules

- All formal endpoints must call `AuthorizationService.requirePermission(...)` with the exact OpenAPI permission.
- All OpenAPI write endpoints must reject missing/blank `idempotency_key` with `VALIDATION_FAILED`.
- Return device with `has_pending_tasks=true` must return conflict code `RETURN_BLOCKED_PENDING_TASKS` and must not mark the device returned.
- Event batch with empty events must return `VALIDATION_FAILED`.
- List evidence events may accept `limit`, cap it to a safe max such as 200, and return the latest events in Android-compatible list shape.
- SQL must use `NamedParameterJdbcTemplate` with named parameters only.

## 8. Audit Events

Use exact OpenAPI event names:

- `terminal.device.bound` / `terminal.device.bind_failed`
- `terminal.device.returned` / `terminal.device.returned_failed`
- `terminal.device.unbound` / `terminal.device.unbind_failed`
- `evidence.events_reported` / `evidence.events_report_failed`
- `evidence.event_viewed` / `evidence.event_view_failed`

## 9. TDD Order

1. Add failing controller tests:
   - unauthenticated bind is rejected
   - bind requires `terminal.device.bind`
   - bind/return/event report missing `idempotency_key` returns `VALIDATION_FAILED`
   - return with pending tasks returns `RETURN_BLOCKED_PENDING_TASKS`
2. Add failing service tests:
   - bind is idempotent and writes `terminal.device.bound`
   - return changes status to `returned`
   - unbind changes status to `unbound`
   - event batch stores all events and writes `evidence.events_reported`
   - event list writes `evidence.event_viewed`
3. Add failing JDBC integration tests:
   - V007 tables exist and permission seed is present
   - device bind/return persists
   - evidence event list returns latest events
4. Implement minimal controllers/services/repositories.
5. Update Android request serialization tests and smoke workflow.
6. Remove replaced smoke handlers.

## 10. Verification

Run, in order:

- `cd /home/james/complex-management/backend && mvn -q -Dtest=TerminalDeviceServiceTests,TerminalDeviceControllerTests,TerminalDeviceJdbcIntegrationTests,EvidenceEventServiceTests,EvidenceEventControllerTests,EvidenceEventJdbcIntegrationTests test`
- `/opt/complex/scripts/complex-verify-p0.sh`

Expected result:

- Backend tests pass.
- `verify:p0-contract` passes.
- Android P0 smoke still passes and no longer depends on device/evidence smoke handlers.
- P0 verification finishes with `P0 verification PASS`.

## 11. Risk And Rollback

Risk:

- Android Kotlin serialization may still emit camelCase unless `@SerialName` is used correctly.
- Existing smoke output expects `deviceType` and `deviceCode` response fields; formal response must remain Android-compatible while accepting OpenAPI request fields.
- Event payload must store arbitrary JSON without logging secrets in normal application logs.

Rollback:

- Create a tar snapshot of `/home/james/complex-management` before editing.
- If full P0 verification fails and cannot be fixed quickly, restore the snapshot and redeploy the previous release.

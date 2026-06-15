# P0 Amusement Safety Session Verification Slice Plan

## 1. Objective

Implement the P0 amusement domain slice on 192.168.88.160 with Java 21 + Spring Boot 4.x + PostgreSQL + Flyway.

This slice must turn the OpenAPI amusement contract into executable backend code for safety templates, pre-opening safety inspections, rectification/recheck/reopen approval, amusement projects, project rules, sessions/capacity, verification, manual override, queue/reservation/device status reads, permissions, idempotency, and audit events.

## 2. In Scope

Formalize these OpenAPI P0 paths:

- `GET /api/v1/amusement/device-statuses`
- `GET /api/v1/amusement/manual-overrides`
- `POST /api/v1/amusement/manual-overrides`
- `GET /api/v1/amusement/projects`
- `POST /api/v1/amusement/projects`
- `GET /api/v1/amusement/projects/{id}/open-eligibility`
- `POST /api/v1/amusement/projects/{id}/rules`
- `GET /api/v1/amusement/queue-entries`
- `GET /api/v1/amusement/reservations`
- `GET /api/v1/amusement/safety-inspections`
- `POST /api/v1/amusement/safety-inspections`
- `POST /api/v1/amusement/safety-inspections/{id}/approve-reopen`
- `POST /api/v1/amusement/safety-inspections/{id}/recheck`
- `POST /api/v1/amusement/safety-inspections/{id}/rectify`
- `POST /api/v1/amusement/safety-inspections/{id}/submit`
- `GET /api/v1/amusement/safety-templates`
- `POST /api/v1/amusement/safety-templates`
- `GET /api/v1/amusement/sessions`
- `POST /api/v1/amusement/sessions`
- `GET /api/v1/amusement/verifications`
- `POST /api/v1/amusement/verifications`

## 3. Out of Scope

- P2 advanced reservation/queue optimization, group booking, package booking, and group-level amusement BI.
- Real robot/device supplier SDK integration. P0 stores device status and supplier boundary fields only.
- Complex risk model scoring. P0 uses deterministic safety/capacity/rule gates.
- Offline high-value verification. P0 amusement verification remains online.

## 4. Files To Add Or Modify

Add:

- `backend/src/main/resources/db/migration/V014__p0_amusement_safety_session_verification.sql`
- `backend/src/main/java/com/complex/platform/amusement/AmusementController.java`
- `backend/src/main/java/com/complex/platform/amusement/AmusementService.java`
- `backend/src/main/java/com/complex/platform/amusement/AmusementRepository.java`
- `backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java`
- `backend/src/main/java/com/complex/platform/amusement/*Record.java`
- `backend/src/main/java/com/complex/platform/amusement/*Request.java`
- `backend/src/test/java/com/complex/platform/amusement/AmusementControllerTests.java`
- `backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java`
- `backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java`

Modify only if required:

- `backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java` if it still asserts latest Flyway version.
- OpenAPI generated closure/matrix files only if contract generation requires sync; do not invent undocumented endpoints.

## 5. Data Model

Add PostgreSQL tables with idempotent Flyway DDL:

- `amusement_safety_templates`
  - `id`, `template_name`, `items_json`, `effective_from`, `status`, `evidence_attachment_ids`, `created_at`, `updated_at`
- `amusement_projects`
  - `id`, `project_name`, `capacity`, `open_status`, `safety_template_id`, `eligibility_rule_json`, `created_at`, `updated_at`
- `amusement_project_rules`
  - `id`, `project_id`, `rule_type`, `rule_value`, `effective_from`, `effective_to`, `blocking`, `status`, `evidence_attachment_ids`, `created_at`, `updated_at`
- `amusement_sessions`
  - `id`, `project_id`, `start_time`, `end_time`, `capacity`, `used_capacity`, `status`, `evidence_attachment_ids`, `created_at`, `updated_at`
- `amusement_safety_inspections`
  - `id`, `project_id`, `template_id`, `items_json`, `overall_result`, `inspection_status`, `rectification_status`, `reopen_status`, `inspected_at`, `submitted_at`, `rechecked_at`, `approved_at`, `operator_note`, `evidence_attachment_ids`, `created_at`, `updated_at`
- `amusement_safety_actions`
  - `id`, `inspection_id`, `action_type`, `action_result`, `comment`, `attachment_ids`, `created_at`
- `amusement_verifications`
  - `id`, `project_id`, `session_id`, `credential_payload`, `device_id`, `staff_id`, `verification_result`, `failure_reason`, `evidence_attachment_ids`, `created_at`
- `amusement_manual_overrides`
  - `id`, `project_id`, `session_id`, `override_type`, `reason`, `status`, `operator_note`, `evidence_attachment_ids`, `created_at`
- `amusement_device_statuses`
  - `id`, `project_id`, `device_id`, `provider_code`, `status`, `last_seen_at`, `raw_status_json`, `created_at`, `updated_at`
- `amusement_queue_entries`
  - `id`, `project_id`, `session_id`, `customer_id`, `queue_no`, `status`, `created_at`, `updated_at`
- `amusement_reservations`
  - `id`, `project_id`, `session_id`, `customer_id`, `reservation_status`, `created_at`, `updated_at`

Seed permissions and attach to `role_platform_admin`:

- `amusement.device_status.view`
- `amusement.verification.override`
- `amusement.session.view`
- `amusement.session.manage`
- `amusement.safety_inspection.view`
- `amusement.project.manage`
- `amusement.queue.view`
- `amusement.reservation.view`
- `amusement.safety_inspection.create`
- `amusement.safety_inspection.approve_reopen`
- `amusement.safety_inspection.recheck`
- `amusement.safety_inspection.rectify`
- `amusement.safety_inspection.submit`
- `amusement.safety_template.view`
- `amusement.safety_template.manage`
- `amusement.verification.view`
- `amusement.verification.execute`

## 6. Business Rules

- P0 amusement verification is online only.
- `safety-template` items are stored as JSONB and must not be empty.
- `safety-inspection` references an existing project and template.
- Inspection `overall_result=pass` allows normal opening after submit.
- Inspection `overall_result=fail` or `pending_rectification` blocks open eligibility until rectification, recheck, and approval complete.
- `submit` must reject a missing inspection or an already terminal/approved inspection.
- `rectify` records evidence/comment and moves the inspection to `rectification_submitted`.
- `recheck` records recheck result. Only `pass` makes the inspection eligible for reopen approval.
- `approve-reopen` requires recheck pass; otherwise reject with conflict and record failure event.
- `open-eligibility` returns blocked when:
  - project is not `open`;
  - latest inspection is missing;
  - latest inspection is failed/pending rectification/not approved;
  - active blocking project rule exists and is not satisfied in P0 deterministic gate;
  - session capacity is exhausted when a session context is supplied later.
- `verification` must reject when project/session is not open, safety gate is blocked, capacity is exhausted, duplicate idempotency key conflicts, or credential payload is blank.
- `verification` increments used capacity only after successful verification.
- `manual-override` requires explicit reason and evidence-capable audit. `allow` may create a successful override record, but does not bypass permanent safety closure unless the project/session is otherwise reopen-approved.
- Device status is read-only P0 seed/update table; no real device command is sent in this slice.
- Queue and reservation are P0 readable records only; creating advanced queue/reservation flows remains future work.

## 7. Interface Rules

- Every endpoint must call `AuthorizationService.requirePermission(...)` with exact OpenAPI permissions.
- Every write endpoint with `x-idempotency-required: true` must reject missing or blank `idempotency_key` with `VALIDATION_FAILED`.
- Every write operation must go through `IdempotencyService`.
- SQL must use `NamedParameterJdbcTemplate` with named parameters.
- Money is not part of this slice. Do not introduce float/double amount handling.
- Do not introduce legacy `/api/admin/*`, `/api/mobile/*`, or `/api/integration/*` routes.
- Response payloads may use existing `ApiResponse.ok(...)` and `PageResponse`.

## 8. Audit Events

Use exact OpenAPI event names:

- `amusement.device_status.viewed` / `amusement.device_status.viewed_failed`
- `amusement.manual_override.viewed` / `amusement.manual_override.view_failed`
- `amusement.verification_overridden` / `amusement.verification_override_failed`
- `amusement.session.viewed` / `amusement.session.view_failed`
- `amusement.session.changed` / `amusement.session.change_failed`
- `amusement.open_eligibility.viewed` / `amusement.open_eligibility.blocked`
- `amusement.project_rule.changed` / `amusement.project_rule.changed_failed`
- `amusement.queue.viewed` / `amusement.queue.viewed_failed`
- `amusement.reservation.viewed` / `amusement.reservation.viewed_failed`
- `amusement.safety_inspection.viewed` / `amusement.safety_inspection.view_failed`
- `amusement.safety_inspection.created` / `amusement.safety_inspection.create_failed`
- `amusement.safety_inspection.reopened` / `amusement.safety_inspection.reopen_rejected`
- `amusement.safety_inspection.rechecked` / `amusement.safety_inspection.recheck_failed`
- `amusement.safety_inspection.rectification_submitted` / `amusement.safety_inspection.rectification_failed`
- `amusement.safety_inspection.submitted` / `amusement.safety_inspection.blocked`
- `amusement.safety_template.viewed` / `amusement.safety_template.view_failed`
- `amusement.safety_template.changed` / `amusement.safety_template.change_failed`
- `amusement.verification.viewed` / `amusement.verification.view_failed`
- `amusement.verification_succeeded` / `amusement.verification_failed`

## 9. TDD Plan

RED tests first:

- Controller permission tests for all 21 amusement endpoints.
- Controller missing idempotency tests for all write endpoints.
- Service tests for:
  - template requires items;
  - project/session create and list;
  - failed inspection blocks open eligibility;
  - pass inspection submit allows eligibility;
  - fail inspection needs rectify -> recheck pass -> approve reopen;
  - approve reopen rejects before recheck pass;
  - verification rejects closed project, blocked safety, full session, and blank credential;
  - verification increments used capacity only on success;
  - manual override requires reason and records override;
  - all success/failure audit event names match OpenAPI.
- JDBC tests for V014 tables, permissions, seed data, save/list flows, and latest Flyway version if asserted.

GREEN implementation:

- Add V014 migration.
- Add amusement package controller/service/repository/records/DTOs.
- Keep all third-party device/provider behavior as table-backed boundary records.
- Implement deterministic P0 safety and capacity gates only.

Refactor only after targeted tests pass.

## 10. Verification

Run targeted tests:

```bash
cd /home/james/complex-management/backend
mvn -q -Dtest=AmusementServiceTests,AmusementControllerTests,AmusementJdbcIntegrationTests test
```

Run contract and full P0:

```bash
cd /home/james/complex-management
npm run verify:p0-contract
/opt/complex/scripts/complex-verify-p0.sh
```

Security and quality scan:

```bash
cd /home/james/complex-management
grep -R -n -E 'createStatement|Statement|String\\.format|SELECT .*\\+|UPDATE .*\\+|INSERT .*\\+|DELETE .*\\+|Double|double|getDouble|Math\\.round|float|Float' backend/src/main/java/com/complex/platform/amusement backend/src/test/java/com/complex/platform/amusement || true
```

## 11. Rollback

- Before implementation, create a tar snapshot under `/home/james/complex-management-backups/`.
- If the slice fails after V014 migration, restore the snapshot for code and reset the development database only if needed.
- Do not edit or rewrite already applied V001-V013 migrations; add only V014 for this slice.

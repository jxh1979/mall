# P1 Amusement Safety Waiver And Incident Work Orders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the P1 amusement safety notice, electronic waiver acknowledgement, and incident work-order backend loop without changing the P0 amusement inspection and verification boundary.

**Architecture:** Add an independent `amusement` Spring package for P1 safety notice and incident workflows. P0 safety inspection tables remain the pre-open blocking authority; this P1 slice adds visitor-facing/legal acknowledgement and post-incident processing records with idempotency, permissions, and audit events.

**Tech Stack:** Java 21, Spring Boot 4.1, PostgreSQL, Flyway, Spring JDBC, JUnit 5, MockMvc, existing idempotency and audit-event services.

---

## Scope

In scope:
- Create/list amusement safety notices for a project.
- Record electronic safety notice/waiver acknowledgements.
- Create/list/process/close amusement incident work orders.
- Store evidence attachment IDs only as references.
- Seed permissions and platform-admin role permissions.
- Write audit events and enforce idempotency for writes.

Out of scope:
- Legal document rich-text editor and signature provider SDK.
- Insurance claim automation.
- Video platform playback or automatic evidence binding.
- Visitor self-service queue/reservation.

## Files

- [x] Create: `backend/src/main/java/com/complex/platform/amusement/AmusementSafetyController.java`
- [x] Create: `backend/src/main/java/com/complex/platform/amusement/AmusementSafetyService.java`
- [x] Create: `backend/src/main/java/com/complex/platform/amusement/AmusementSafetyRepository.java`
- [x] Create: `backend/src/main/java/com/complex/platform/amusement/JdbcAmusementSafetyRepository.java`
- [x] Create: records and DTOs under `backend/src/main/java/com/complex/platform/amusement/`
- [x] Create: `backend/src/main/resources/db/migration/V024__p1_amusement_safety_waiver_incident.sql`
- [x] Create: `backend/src/test/java/com/complex/platform/amusement/AmusementSafetyServiceTests.java`
- [x] Create: `backend/src/test/java/com/complex/platform/amusement/AmusementSafetyControllerTests.java`
- [x] Create: `backend/src/test/java/com/complex/platform/amusement/AmusementSafetyJdbcIntegrationTests.java`
- [x] Create: `docs/p1-amusement-safety-waiver-incident-api-contract-v0.1.md`
- [x] Modify: JDBC integration tests with fixed Flyway version assertions from `023` to `024`.

## API/Permission/Event Contract

- `GET /api/v1/amusement/safety-notices`
  - Permission: `amusement.safety_notice.view`
  - Event: `amusement.safety_notice.viewed`
- `POST /api/v1/amusement/safety-notices`
  - Permission: `amusement.safety_notice.manage`
  - Success event: `amusement.safety_notice.created`
  - Failure event: `amusement.safety_notice.create_failed`
- `POST /api/v1/amusement/safety-notices/{notice_id}/acknowledgements`
  - Permission: `amusement.waiver.acknowledge`
  - Success event: `amusement.waiver.acknowledged`
  - Failure event: `amusement.waiver.acknowledge_failed`
- `GET /api/v1/amusement/incidents`
  - Permission: `amusement.incident.view`
  - Event: `amusement.incident.viewed`
- `POST /api/v1/amusement/incidents`
  - Permission: `amusement.incident.create`
  - Success event: `amusement.incident.created`
  - Failure event: `amusement.incident.create_failed`
- `POST /api/v1/amusement/incidents/{id}/actions`
  - Permission: `amusement.incident.process`
  - Success event: `amusement.incident.action_added`
  - Failure event: `amusement.incident.action_failed`
- `POST /api/v1/amusement/incidents/{id}/close`
  - Permission: `amusement.incident.close`
  - Success event: `amusement.incident.closed`
  - Failure event: `amusement.incident.close_failed`

## Acceptance Criteria

- A safety notice can be created and listed by project.
- A waiver acknowledgement requires notice, project, customer or visitor, signer name, acknowledgement method, and content version.
- A missing notice rejects acknowledgement with `SAFETY_NOTICE_NOT_FOUND`.
- An incident can be created with severity, project, optional session/device/customer, title, description, and evidence references.
- Incident processing writes immutable action records and can transition to `processing`, `pending_external`, or `resolved`.
- Closed incidents reject later processing with `INCIDENT_CLOSED`.
- Writes are idempotent and audited.
- Permissions are enforced at Controller level.
- Flyway V024 creates tables, indexes, checks, permissions, and role permissions.
- Full backend tests, P0 contract verification, and focused Semgrep pass.

## Risks And Rollback

- Risk: Real legal waiver provider later requires more fields. Mitigation: store `content_version`, signer, method, evidence references, and content hash placeholder; provider fields can be added later.
- Risk: Incident workflow overlaps merchant-service work orders. Mitigation: keep amusement incidents in the amusement domain because they carry project/session/device/severity and safety-specific closure fields.
- Risk: Video/insurance integrations are not ready. Mitigation: store external evidence references only.
- Rollback: remove V024 before application in clean environments. If already applied, use a forward migration to disable permissions and archive records instead of dropping active tables.

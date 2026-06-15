# P0 Foundation Residual Platform Terminal Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close remaining P0 foundation APIs for platform contracts/entity bindings, terminal switches/kiosk config/heartbeat, and evidence attachments/reviews/timeline.

**Architecture:** Extend existing `governance`, `terminal`, and `evidence` packages with formal Controller/DTO/Service/Repository/Flyway implementations. Keep each domain separate, share only `IdempotencyService`, `AuthorizationService`, and `AuditEventService`.

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL, Flyway, NamedParameterJdbcTemplate, Maven, JUnit 6, MockMvc.

---

## Scope

Implement the remaining P0 foundation operations:

Platform:

- `GET /api/v1/platform/business-entity-bindings`
- `POST /api/v1/platform/business-entity-bindings`
- `POST /api/v1/platform/business-entity-bindings/{id}/change`
- `GET /api/v1/platform/contracts`
- `POST /api/v1/platform/contracts`
- `POST /api/v1/platform/contracts/{contract_id}/approve`

Terminals:

- `GET /api/v1/terminals/capability-switches`
- `PUT /api/v1/terminals/capability-switches/{id}`
- `GET /api/v1/terminals/kiosks/{device_id}/config`
- `POST /api/v1/terminals/kiosks/{device_id}/heartbeat`

Evidence:

- `POST /api/v1/evidence/attachments/upload-intents`
- `POST /api/v1/evidence/attachments/{attachment_id}/bind`
- `POST /api/v1/evidence/attachments/{attachment_id}/complete`
- `POST /api/v1/evidence/reviews`
- `GET /api/v1/evidence/timelines/{object_type}/{object_id}`

## File Structure

Create:

- `backend/src/main/resources/db/migration/V010__p0_foundation_residual_platform_terminal_evidence.sql`
- New DTOs under:
  - `backend/src/main/java/com/complex/platform/governance/dto/`
  - `backend/src/main/java/com/complex/platform/terminal/dto/`
  - `backend/src/main/java/com/complex/platform/evidence/dto/`
- New records/responses in `governance`, `terminal`, and `evidence`.
- New/expanded tests:
  - `backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java`
  - `backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java`
  - `backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java`
  - `backend/src/test/java/com/complex/platform/terminal/TerminalDeviceServiceTests.java`
  - `backend/src/test/java/com/complex/platform/terminal/TerminalDeviceControllerTests.java`
  - `backend/src/test/java/com/complex/platform/terminal/TerminalDeviceJdbcIntegrationTests.java`
  - `backend/src/test/java/com/complex/platform/evidence/EvidenceEventServiceTests.java`
  - `backend/src/test/java/com/complex/platform/evidence/EvidenceEventControllerTests.java`
  - `backend/src/test/java/com/complex/platform/evidence/EvidenceEventJdbcIntegrationTests.java`

Modify:

- `PlatformController.java`, `PlatformService.java`, `PlatformRepository.java`, `JdbcPlatformRepository.java`
- `TerminalDeviceController.java`, `TerminalDeviceService.java`, `TerminalDeviceRepository.java`, `JdbcTerminalDeviceRepository.java`
- `EvidenceEventController.java`, `EvidenceEventService.java`, `EvidenceEventRepository.java`, `JdbcEvidenceEventRepository.java`

Do not implement bar, inventory, amusement, integration, or analytics business logic in this slice.

## Data Structures

Add Flyway V010 tables:

- `business_entity_bindings`: operating object, operating entity, contract entity, settlement entity, merchant/project scope, status, effective dates, change reason.
- `contracts`: contract draft/approval status, merchant, project, contract subject, effective dates, key date summary, attachment ids JSONB.
- `terminal_capability_switches`: terminal type, capability code, scope, enabled flag, reason, updated metadata.
- `terminal_kiosk_configs`: device id, kiosk mode (`ticketing` or `service`), ticketing enabled flag, bound scope, config JSONB.
- `terminal_kiosk_heartbeats`: device id, status, app version, ip/network summary, last heartbeat.
- `evidence_attachments`: object storage key, upload url placeholder, status, content type, size, checksum, bound object fields.
- `evidence_reviews`: object type/id, risk level, decision, reason, evidence ids, status.

Use existing `audit_events`/`evidence_events` for timeline composition.

Seed missing permissions for contracts, terminal switches, and evidence attachment/review/timeline if not already present, granting `role_platform_admin` for 160 tests.

## TDD Tasks

### Task 1: Platform Entity Bindings

- [ ] Write failing tests proving only one active binding is allowed per operating object.
- [ ] Run tests and confirm RED.
- [ ] Implement list/create/change binding with exact events:
  - `entity_binding.viewed` / `entity_binding.view_failed`
  - `entity_binding.changed` / `entity_binding.change_failed`
- [ ] Add JDBC integration coverage for uniqueness and status history.
- [ ] Run targeted platform tests.

### Task 2: Platform Contract Draft And Approval

- [ ] Write failing tests proving draft persists attachment ids/key dates and approval changes lifecycle status without deleting the draft.
- [ ] Run tests and confirm RED.
- [ ] Implement list/create/approve contract with exact permissions/events:
  - `contract.view`, `contract.viewed` / `contract.view_failed`
  - `contract.draft.manage`, `contract.draft_saved` / `contract.draft_save_failed`
  - `contract.lifecycle.approve`, `contract.lifecycle_changed` / `contract.lifecycle_change_failed`
- [ ] Run targeted platform tests.

### Task 3: Terminal Capability Switches

- [ ] Write failing tests proving capability switch read/write persists enabled state and writes audit events.
- [ ] Run tests and confirm RED.
- [ ] Implement routes under `/api/v1/terminals/capability-switches`.
- [ ] Exact events:
  - `terminal_switch.viewed` / `terminal_switch.view_failed`
  - `terminal_switch.changed` / `terminal_switch.change_failed`
- [ ] Run targeted terminal tests.

### Task 4: Kiosk Config And Heartbeat

- [ ] Write failing tests proving ticketing kiosk returns ticketing-enabled config, service kiosk does not expose ticketing, and heartbeat updates last seen status.
- [ ] Run tests and confirm RED.
- [ ] Implement `/api/v1/terminals/kiosks/{device_id}/config` and heartbeat without staff permission, but with explicit device id validation and evidence/audit writes.
- [ ] Exact events:
  - `terminal.kiosk_config_viewed` / `terminal.access_rejected`
  - `terminal.kiosk_heartbeat` / `terminal.kiosk_heartbeat_failed`
- [ ] Run targeted terminal tests and Android smoke-compatible tests.

### Task 5: Evidence Attachments

- [ ] Write failing tests proving upload intent creates a pending attachment with placeholder upload url/key, bind attaches it to an object, complete marks uploaded and stores checksum/size.
- [ ] Run tests and confirm RED.
- [ ] Implement attachment DTOs/records/repository/service/controller routes.
- [ ] Exact events:
  - `evidence.upload_intent_created` / `evidence.upload_intent_failed`
  - `evidence.attachment_bound` / `evidence.attachment_bind_failed`
  - `evidence.attachment_completed` / `evidence.attachment_complete_failed`
- [ ] Run targeted evidence tests.

### Task 6: Evidence Review And Timeline

- [ ] Write failing tests proving review persists object/risk/decision/evidence ids, and timeline returns audit events, evidence events, attachment events, and reviews for object type/id.
- [ ] Run tests and confirm RED.
- [ ] Implement review and timeline read model.
- [ ] Exact events:
  - `evidence.review_created` / `evidence.review_create_failed`
  - `evidence.timeline_viewed` / `evidence.timeline_view_failed`
- [ ] Run targeted evidence tests.

### Task 7: Verification And Review

- [ ] Run:

```bash
cd /home/james/complex-management/backend && mvn -q -Dtest=PlatformServiceTests,PlatformControllerTests,PlatformJdbcIntegrationTests,TerminalDeviceServiceTests,TerminalDeviceControllerTests,TerminalDeviceJdbcIntegrationTests,EvidenceEventServiceTests,EvidenceEventControllerTests,EvidenceEventJdbcIntegrationTests test
```

- [ ] Run:

```bash
npm run verify:p0-contract
```

- [ ] Run:

```bash
/opt/complex/scripts/complex-verify-p0.sh
```

- [ ] Review for missing permissions, missing idempotency, event name mismatches, SQL string interpolation, and customer/device/staff auth boundary confusion.
- [ ] Confirm Flyway latest version is `010`.

## Acceptance Criteria

- All scoped OpenAPI paths exist in formal Java controllers.
- Staff/admin operations use exact permission codes.
- Kiosk config/heartbeat do not accidentally require staff token, but validate device identity path and write audit events.
- All scoped write operations require idempotency where OpenAPI requires it.
- All success/failure event names match `docs/openapi/openapi-p0.json`.
- Entity binding prevents a second active binding for the same operating object.
- Contract approval changes lifecycle status and preserves attachments.
- Capability switches determine whether kiosk ticketing is exposed.
- Evidence upload, bind, complete, review, and timeline can be verified in JDBC integration tests.
- Full P0 verification passes on 160.

## Risks And Rollback

- Kiosk endpoints can accidentally bypass all device checks. Keep P0 validation explicit: known `device_id`, known config, rejected unknown devices.
- Entity binding uniqueness can block test fixtures if existing data is not isolated. Use idempotent ids and `ON CONFLICT` carefully.
- Timeline joins can become overcomplicated. P0 accepts a merged read model with stable fields rather than a complex event-sourcing engine.

Rollback command pattern:

```bash
tar -xzf /home/james/complex-management-backups/<snapshot>.tgz -C /home/james/complex-management --strip-components=1
/opt/complex/scripts/complex-verify-p0.sh
```

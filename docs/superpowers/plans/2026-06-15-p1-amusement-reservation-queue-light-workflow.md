# P1 Amusement Reservation Queue Light Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the P1 amusement reservation and queue light backend loop with write APIs, status transitions, permissions, audit events, and JDBC persistence.

**Architecture:** Extend the existing `amusement` Spring package. Existing V014 tables already store `amusement_reservations` and `amusement_queue_entries`; V032 only seeds write permissions. Dynamic capacity, group booking, package booking, and cross-project group view remain out of this slice.

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL, Flyway, JUnit, MockMvc.

---

### Task 1: Reservation And Queue Request Models

**Files:**
- Create: `backend/src/main/java/com/complex/platform/amusement/AmusementReservationRequest.java`
- Create: `backend/src/main/java/com/complex/platform/amusement/AmusementQueueEntryRequest.java`

- [ ] Add DTOs with `idempotencyKey`, `projectId`, `sessionId`, `customerId`, optional `reservationStatus`, optional `queueNo`, and optional `status`.

### Task 2: Service Behavior

**Files:**
- Modify: `backend/src/main/java/com/complex/platform/amusement/AmusementService.java`
- Modify: `backend/src/main/java/com/complex/platform/amusement/AmusementRepository.java`
- Modify: `backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java`
- Test: `backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java`

- [ ] Write failing service tests for reservation create, reservation status change, queue join, queue status change, session-project mismatch, and missing record.
- [ ] Implement minimal service methods:
  - `createReservation`
  - `changeReservationStatus`
  - `createQueueEntry`
  - `changeQueueEntryStatus`
- [ ] Add repository `findReservationById` and `findQueueEntryById`.
- [ ] Emit audit success/failure events for every write.

### Task 3: Controller Routes And Permissions

**Files:**
- Modify: `backend/src/main/java/com/complex/platform/amusement/AmusementController.java`
- Modify: `backend/src/test/java/com/complex/platform/amusement/AmusementControllerTests.java`
- Create: `backend/src/main/resources/db/migration/V032__p1_amusement_reservation_queue_write_permissions.sql`

- [ ] Add `POST /api/v1/amusement/reservations`, `POST /api/v1/amusement/reservations/{id}/status`, `POST /api/v1/amusement/queue-entries`, and `POST /api/v1/amusement/queue-entries/{id}/status`.
- [ ] Require `amusement.reservation.manage` and `amusement.queue.manage`.
- [ ] Seed both permissions and assign them to `role_platform_admin`.
- [ ] Update Flyway version expectations from `031` to `032`.

### Task 4: API Contract And Verification

**Files:**
- Create: `docs/p1-amusement-reservation-queue-light-api-contract-v0.1.md`

- [ ] Document endpoints, permissions, request fields, status values, idempotency, audit events, and out-of-scope capabilities.
- [ ] Verify red-green on 160 with `mvn -Dtest=AmusementServiceTests,AmusementControllerTests,AmusementJdbcIntegrationTests test`.

**Acceptance Criteria:**
- Reservation create persists a `reserved` reservation for a valid project/session/customer and is idempotent.
- Queue join persists a `waiting` queue entry and is idempotent.
- Status changes are explicit, permission-protected, and auditable.
- Cross-project session mismatch is rejected.
- Closed/cancelled session cannot accept new reservation or queue entry.
- V032 seeds new write permissions without changing V014 data tables.

**Risks And Rollback:**
- Risk: Scope creep into dynamic capacity and group booking. Mitigation: keep this slice to light CRUD/status workflow only.
- Risk: Flyway version drift across existing JDBC tests. Mitigation: update all latest-version assertions together.
- Rollback before deployment: remove V032, request DTOs, service/controller methods, tests, and API contract doc.
- Rollback after deployment: forward migration can disable `amusement.reservation.manage` and `amusement.queue.manage` while preserving historical rows.

# P1 Floor Plan Image And Polygon Coordinates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the P1 mall floor-plan backend loop for image-based floor plans and normalized polygon coordinates.

**Architecture:** Add an independent `floorplan` Spring package that stores floor plan metadata and reusable polygon shapes. Existing `spaces.polygon_points` remains a P0 lightweight field; this P1 module becomes the normalized floor-plan source for future merchant, amusement, device, video, and robot references.

**Tech Stack:** Java 21, Spring Boot 4.1, PostgreSQL, Flyway, Spring JDBC, JUnit 5, MockMvc, existing idempotency and audit event services.

---

## Scope

In scope:
- Create/list floor plans with image URL, dimensions, floor number, project ID, version, and status.
- Create/update/list polygon shapes for a floor plan.
- Store polygon points as normalized coordinates where `x` and `y` are between 0 and 1.
- Seed permissions and platform-admin role permissions.
- Write audit events and use idempotency for writes.

Out of scope:
- CAD/SVG standardized import.
- Route planning, robot navigation, video point deep linkage, emergency evacuation.
- File upload/storage; image URL and attachment management remain external.

## Files

- Create: `backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java`
- Create: `backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java`
- Create: `backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java`
- Create: `backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java`
- Create: `backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java`
- Create: `backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java`
- Create: `backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java`
- Create: `backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java`
- Create: `backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java`
- Create: `backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql`
- Create: `backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java`
- Create: `backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java`
- Create: `backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java`
- Create: `docs/p1-floor-plan-polygon-api-contract-v0.1.md`
- Modify: JDBC integration tests with fixed Flyway version assertions from `022` to `023`.

## API/Permission/Event Contract

- `GET /api/v1/floor-plans`
  - Permission: `floor_plan.view`
  - Event: `floor_plan.viewed`
- `POST /api/v1/floor-plans`
  - Permission: `floor_plan.manage`
  - Success event: `floor_plan.created`
  - Failure event: `floor_plan.create_failed`
- `GET /api/v1/floor-plans/{id}/shapes`
  - Permission: `floor_plan.view`
  - Event: `floor_plan.shape_viewed`
- `POST /api/v1/floor-plans/{id}/shapes`
  - Permission: `floor_plan.manage`
  - Success event: `floor_plan.shape_saved`
  - Failure event: `floor_plan.shape_save_failed`

## Data Contract

`mall_floor_plans`:
- `id varchar(64) primary key`
- `project_id varchar(64)`
- `building_code varchar(64)`
- `floor_no varchar(64) not null`
- `plan_name varchar(160) not null`
- `image_url varchar(500) not null`
- `image_width integer not null`
- `image_height integer not null`
- `version_no varchar(64) not null`
- `status varchar(32) not null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

`floor_plan_shapes`:
- `id varchar(64) primary key`
- `floor_plan_id varchar(64) not null references mall_floor_plans(id)`
- `shape_type varchar(32) not null`
- `ref_type varchar(32) not null`
- `ref_id varchar(64)`
- `label varchar(160) not null`
- `polygon_points jsonb not null`
- `z_index integer not null default 0`
- `status varchar(32) not null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

## TDD Tasks

### Task 1: Service Behavior

- [x] Write failing service tests for creating floor plans, rejecting bad dimensions, saving polygons, rejecting polygons with fewer than 3 points, rejecting out-of-range normalized points, and idempotency conflict/replay.
- [x] Run `cd /home/james/complex-management/backend && mvn -Dtest=FloorPlanServiceTests test` and confirm RED.
- [x] Implement minimal records, DTOs, service, and repository interface.
- [x] Re-run the same command and confirm GREEN.

### Task 2: Controller Contract

- [x] Write failing MockMvc tests for list/create/list-shapes/save-shape permissions, idempotency key validation, and response shape.
- [x] Run `cd /home/james/complex-management/backend && mvn -Dtest=FloorPlanControllerTests test` and confirm RED.
- [x] Implement Controller.
- [x] Re-run the same command and confirm GREEN.

### Task 3: JDBC Persistence

- [x] Write failing integration tests for V023 tables, permissions, and round-trip floor plan/shape persistence.
- [x] Run `cd /home/james/complex-management/backend && mvn -Dtest=FloorPlanJdbcIntegrationTests test` and confirm RED.
- [x] Implement V023 migration and JDBC repository.
- [x] Re-run `cd /home/james/complex-management/backend && mvn -Dtest=FloorPlanServiceTests,FloorPlanControllerTests,FloorPlanJdbcIntegrationTests test` and confirm GREEN.

### Task 4: Documentation And Gates

- [x] Create `docs/p1-floor-plan-polygon-api-contract-v0.1.md`.
- [x] Run `cd /home/james/complex-management/backend && mvn test`.
- [x] Run `cd /home/james/complex-management && npm run verify:p0-contract`.
- [x] Run focused Semgrep on `backend/src/main/java/com/complex/platform/floorplan` and `V023__p1_floor_plan_polygons.sql`.

## Acceptance Criteria

- A floor plan can be created and listed with image URL, dimensions, floor, version, and status.
- A shape can be saved for a floor plan with shape type, reference type, label, z-index, status, and at least 3 normalized points.
- A point outside `[0, 1]` is rejected.
- A shape cannot be saved to a missing floor plan.
- Writes are idempotent and audited.
- Permissions are enforced at Controller level.
- Flyway migration V023 creates tables, indexes, checks, permissions, and role permissions.
- Full backend tests, P0 contract verification, and focused Semgrep pass.

## Risks And Rollback

- Risk: P1 normalized coordinates may need pixel coordinates for a vendor tool later. Mitigation: store image dimensions and document normalized coordinates as canonical; vendor import can transform later.
- Risk: P0 `spaces.polygon_points` duplicates some shape data. Mitigation: P0 field remains lightweight and backward compatible; P1 floor-plan shapes become the richer source for map UIs.
- Risk: File upload is not implemented here. Mitigation: store `image_url`; evidence/object storage remains separate.
- Rollback: remove V023 before application in clean environments. If already applied, use a forward migration to disable permissions and archive records instead of dropping active tables.

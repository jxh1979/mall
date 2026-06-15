# P0 平台主体与空间建档正式化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 P0 smoke 中临时承接的 `/api/v1/platform/business-entities`、`/api/v1/platform/merchants`、`/api/v1/platform/projects`、`/api/v1/platform/spaces` 迁入正式 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 实现。

**Architecture:** 新增 `com.complex.platform.governance` 领域，使用 Flyway 表保存主体、商户、项目和空间。Controller 入口做 operation 级权限校验和幂等校验，Service 负责字段规范化、审计事件和幂等执行，Repository 使用 `NamedParameterJdbcTemplate` 参数化 SQL。迁移完成后，从 `P0SmokeController` 移除同路径平台临时映射，避免正式 Controller 与 smoke Controller 重复映射。

**Tech Stack:** Java 21, Spring Boot 4.1.0, Maven 3.9+, PostgreSQL, Flyway, Spring Security, Spring MVC, JUnit 5, AssertJ, MockMvc.

---

## 1. Scope

### In Scope

- `GET /api/v1/platform/business-entities` / `listBusinessEntities`
- `POST /api/v1/platform/business-entities` / `createBusinessEntity`
- `GET /api/v1/platform/merchants` / `listMerchants`
- `POST /api/v1/platform/merchants` / `createMerchant`
- `PUT /api/v1/platform/merchants/{merchant_id}` / `updateMerchant`
- `GET /api/v1/platform/projects` / `listProjects`
- `POST /api/v1/platform/projects` / `createProject`
- `GET /api/v1/platform/spaces` / `listSpaces`
- `POST /api/v1/platform/spaces` / `createSpace`
- 权限种子补齐：`entity.binding.view/manage`、`merchant.profile.view/manage`、`governance.project.view/manage`、`governance.space.view/manage`
- 成功/失败事件：`entity_binding.viewed/update_failed/updated`、`merchant.viewed/update_failed/updated`、`project.viewed/update_failed/updated`、`space.viewed/update_failed/updated`
- Android P0 fixture 对平台建档字段与 OpenAPI request schema 对齐，仍必须通过 `android:p0:smoke`。

### Out of Scope

- 完整合同审批和附件归档。
- 四类主体唯一绑定的复杂版本流转。
- 数据范围 ABAC 深化。
- 商场 CAD/SVG 导入。
- P1 商户服务工单和商户账单增强。

## 2. File Structure

### Create

- `backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql`
- `backend/src/main/java/com/complex/platform/governance/PlatformController.java`
- `backend/src/main/java/com/complex/platform/governance/PlatformService.java`
- `backend/src/main/java/com/complex/platform/governance/PlatformRepository.java`
- `backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java`
- `backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java`
- `backend/src/main/java/com/complex/platform/governance/MerchantRecord.java`
- `backend/src/main/java/com/complex/platform/governance/ProjectRecord.java`
- `backend/src/main/java/com/complex/platform/governance/SpaceRecord.java`
- `backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java`
- `backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java`
- `backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java`
- `backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java`
- `backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java`
- `backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java`
- `backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java`

### Modify

- `backend/src/main/java/com/complex/platform/smoke/P0SmokeController.java`
  - Remove only the platform path handlers that now have formal implementations.
  - Keep ticketing, terminal, evidence and device smoke handlers until their own formal slices replace them.
- `apps/android-terminal/src/main/kotlin/com/complex/terminal/P0Fixture.kt`
  - Use OpenAPI-aligned platform request fields: `entity_name`, `entity_role`, `merchant_name`, `merchant_type`, `business_entity_id`, `venue_type`, `space_name`, `space_type`, `parent_space_id`.
  - Keep stable `idempotency_key` and `operator_note`.

## 3. Data Structure

`V005__p0_platform_master_data.sql` must be forward-compatible:

- `business_entities`
  - `id varchar(64) primary key`
  - `entity_name varchar(120) not null`
  - `entity_role varchar(32) not null`
  - `unified_social_credit_code varchar(128)`
  - `legal_representative varchar(255)`
  - `bank_account_ref text`
  - `status varchar(32) not null`
  - `created_at timestamptz not null`
  - `updated_at timestamptz not null`
- `merchants`
  - `id varchar(64) primary key`
  - `merchant_name varchar(120) not null`
  - `merchant_type varchar(32) not null`
  - `business_entity_id varchar(64)`
  - `contract_entity_id varchar(64)`
  - `settlement_entity_id varchar(64)`
  - `contact_name varchar(120)`
  - `contact_phone varchar(20)`
  - `status varchar(32) not null`
  - `created_at timestamptz not null`
  - `updated_at timestamptz not null`
- `projects`
  - `id varchar(64) primary key`
  - `merchant_id varchar(64)`
  - `venue_type varchar(64) not null`
  - `name varchar(120) not null`
  - `code varchar(64)`
  - `floor varchar(32)`
  - `capacity integer`
  - `status varchar(32) not null`
  - `created_at timestamptz not null`
  - `updated_at timestamptz not null`
- `spaces`
  - `id varchar(64) primary key`
  - `project_id varchar(64)`
  - `merchant_id varchar(64)`
  - `parent_space_id varchar(64)`
  - `space_name varchar(120) not null`
  - `space_type varchar(64) not null`
  - `floor_no varchar(128)`
  - `area_square_meter numeric(18,2)`
  - `polygon_points jsonb not null default '[]'::jsonb`
  - `status varchar(32) not null`
  - `created_at timestamptz not null`
  - `updated_at timestamptz not null`

Seed permissions with `INSERT ... ON CONFLICT DO UPDATE`, and add all new permissions to `role_platform_admin`.

## 4. TDD Tasks

### Task 1: Flyway and JDBC Repository

**Files:**

- Create: `backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql`
- Create: `backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java`
- Create: `backend/src/main/java/com/complex/platform/governance/PlatformRepository.java`
- Create: `backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java`
- Create records under `backend/src/main/java/com/complex/platform/governance/`

- [ ] **Step 1: Write failing integration tests**

Tests:

- `migrationCreatesPlatformMasterDataTablesAndSeedsPermissions`
- `createAndListBusinessEntityPersistsFormalFields`
- `createUpdateAndListMerchantPersistsFormalFields`
- `createProjectAndSpacePersistsHierarchyFields`

- [ ] **Step 2: Run red test**

Run: `npm run test:backend`

Expected: fails because `PlatformRepository` and V005 are not implemented.

- [ ] **Step 3: Minimal implementation**

Implement repository with `NamedParameterJdbcTemplate`; all user-controlled values must be query parameters. Convert `Instant` to UTC `OffsetDateTime`.

- [ ] **Step 4: Run green test**

Run: `npm run test:backend`

Expected: backend tests pass.

### Task 2: Platform Service

**Files:**

- Create: `backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java`
- Create: `backend/src/main/java/com/complex/platform/governance/PlatformService.java`
- Create DTOs under `backend/src/main/java/com/complex/platform/governance/dto/`

- [ ] **Step 1: Write failing service tests**

Tests:

- `createMerchantRequiresFormalMerchantName`
- `createBusinessEntityUsesIdempotencyAndWritesAudit`
- `updateMerchantWritesAuditAndPreservesId`
- `listSpacesWritesViewedAudit`

- [ ] **Step 2: Run red test**

Run: `npm run test:backend`

Expected: fails because Service behavior is absent.

- [ ] **Step 3: Minimal implementation**

Use `IdempotencyService.execute(...)` for write operations. Validate required fields manually until Jakarta Bean Validation is introduced consistently. Record success/failure audit events with exact event names from OpenAPI closure appendix.

- [ ] **Step 4: Run green test**

Run: `npm run test:backend`

Expected: backend tests pass.

### Task 3: Platform Controller and Smoke Controller Handoff

**Files:**

- Create: `backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java`
- Create: `backend/src/main/java/com/complex/platform/governance/PlatformController.java`
- Modify: `backend/src/main/java/com/complex/platform/smoke/P0SmokeController.java`

- [ ] **Step 1: Write failing controller tests**

Tests:

- unauthenticated platform list returns 401/403.
- creating merchant without `merchant.profile.manage` returns `PERMISSION_DENIED`.
- creating merchant without `idempotency_key` returns `VALIDATION_FAILED`.
- listing merchants returns `ApiResponse<PageResponse<MerchantRecord>>`.
- creating space uses `governance.space.manage`.

- [ ] **Step 2: Run red test**

Run: `npm run test:backend`

Expected: fails because formal Controller is absent or duplicate smoke mapping exists.

- [ ] **Step 3: Minimal implementation**

Create Controller methods matching OpenAPI paths. Remove only these platform path handlers from `P0SmokeController`:

- `createBusinessEntity`
- `createMerchant`
- `createProject`
- `createSpace`

Do not remove ticketing, terminal, evidence or return-device smoke handlers.

- [ ] **Step 4: Run green test**

Run: `npm run test:backend`

Expected: backend tests pass and application context has no duplicate mapping failure.

### Task 4: Android P0 Fixture Contract Alignment

**Files:**

- Modify: `apps/android-terminal/src/main/kotlin/com/complex/terminal/P0Fixture.kt`
- Modify or create: `apps/android-terminal/src/test/kotlin/com/complex/terminal/P0FixtureTest.kt`

- [ ] **Step 1: Write failing fixture assertion**

Extend `P0FixtureTest` to assert platform fixture request bodies include OpenAPI-aligned field names:

- business entity: `entity_name`, `entity_role`
- merchant: `merchant_name`, `merchant_type`, `business_entity_id`
- project: `name`, `venue_type`
- space: `space_name`, `space_type`

- [ ] **Step 2: Run red test**

Run: `node tools/run-android-gradle.mjs --project apps/android-terminal test --tests com.complex.terminal.P0FixtureTest`

Expected: fails on old fixture field names.

- [ ] **Step 3: Minimal implementation**

Update fixture body fields. Use `merchant_type = "other"` for P0 platform setup, because OpenAPI enum does not include `platform`.

- [ ] **Step 4: Run green test**

Run: `node tools/run-android-gradle.mjs --project apps/android-terminal test --tests com.complex.terminal.P0FixtureTest`

Expected: fixture test passes.

## 5. Acceptance Criteria

- `npm run test:backend` passes.
- `npm run verify:p0-contract` passes.
- `node tools/run-android-gradle.mjs --project apps/android-terminal test --tests com.complex.terminal.P0FixtureTest` passes.
- `/opt/complex/scripts/complex-verify-p0.sh` passes after deployment.
- `POST /api/v1/platform/merchants` without `idempotency_key` returns `VALIDATION_FAILED`.
- Authenticated user without `merchant.profile.manage` cannot create merchant.
- Seed platform admin can create business entity, merchant, project and space through formal Controller.
- Android P0 smoke still creates its fixture through formal platform endpoints and temporary smoke ticketing endpoints.
- `P0SmokeController` no longer owns formal platform master-data paths.

## 6. Risk and Rollback

- Risk: duplicate request mappings if smoke controller keeps the same platform paths. Mitigation: remove only the four platform `@PostMapping` methods from smoke after formal Controller exists.
- Risk: Android fixture used legacy field names. Mitigation: update fixture and extend fixture test before implementation is considered complete.
- Risk: permissions missing from seed data causing non-admin users to fail all platform tests. Mitigation: seed platform permission codes in V005 and attach them to `role_platform_admin`.
- Rollback: restore the latest tar snapshot created before this slice; do not use `git reset` because the remote directory is not a valid Git repository.

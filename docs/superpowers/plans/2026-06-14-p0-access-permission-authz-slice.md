# P0 权限模板与操作级授权第二切片 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 192.168.88.160 上实现 P0 首批编码包的第二个正式纵切：权限点字典、权限模板、模板复制/应用、临时授权、最终权限计算、生产前置接口操作级授权和审计留痕。

**Architecture:** 以 `backend/` 为唯一正式后端入口，新增 `com.complex.platform.access` 领域；用 PostgreSQL + Flyway 保存权限权威表，用 `AuthorizationService.requirePermission()` 在 Controller 入口做操作级授权。保留当前 local/dev/test smoke 登录能力，但不能再让任意已登录用户绕过 operation 权限。

**Tech Stack:** Java 21, Spring Boot 4.1.0, Maven 3.9+, PostgreSQL, Flyway, Spring Security, Spring MVC, JUnit 5, AssertJ, MockMvc.

---

## 1. 需求和边界审计结论

- 当前 OpenAPI 已有 access 接口组，不新增路径；本切片必须实现已有契约中的 10 个 operation：`listPermissionTemplates`、`createPermissionTemplate`、`updatePermissionTemplate`、`applyPermissionTemplate`、`copyPermissionTemplate`、`listPermissions`、`listRoles`、`createRole`、`createTemporaryGrant`、`revokeTemporaryGrant`，并补最小 `listAccessUsers` / `createAccessUser` 用于模板应用对象闭环。
- 当前 `ProductionReadinessController` 只有认证，没有操作级授权。本切片必须让 `GET/PUT/POST /api/v1/ops/production-readiness/*` 分别校验 `ops.production_readiness.view/manage/check`。
- P0 权限计算口径固定：角色权限 + 基础模板授权 + 有效临时模板授权 + 员工级 grant + 临时授权 - 员工级 deny - 模板项 deny；deny 优先级最高；临时模板到期或单次消耗后失效。
- 本切片不做菜单树、不做审批流引擎、不做复杂组织人事同步、不做生产级密码体系替换；这些不得阻塞权限模板、授权计算和留痕。
- 当前 160 源码目录不是 Git 仓库，开工前必须做 tar 快照，回滚以快照恢复为准。

## 2. 文件结构

### 新增后端文件

- `backend/src/main/resources/db/migration/V003__p0_access_permission_authz_slice.sql`
- `backend/src/main/java/com/complex/platform/access/AccessController.java`
- `backend/src/main/java/com/complex/platform/access/AccessService.java`
- `backend/src/main/java/com/complex/platform/access/AccessRepository.java`
- `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java`
- `backend/src/main/java/com/complex/platform/access/AuthorizationService.java`
- `backend/src/main/java/com/complex/platform/access/AccessActor.java`
- `backend/src/main/java/com/complex/platform/access/PermissionDefinition.java`
- `backend/src/main/java/com/complex/platform/access/PermissionTemplate.java`
- `backend/src/main/java/com/complex/platform/access/PermissionTemplateItem.java`
- `backend/src/main/java/com/complex/platform/access/PermissionTemplateApplication.java`
- `backend/src/main/java/com/complex/platform/access/TemporaryGrant.java`
- `backend/src/main/java/com/complex/platform/access/RoleRecord.java`
- `backend/src/main/java/com/complex/platform/access/AccessUser.java`
- `backend/src/main/java/com/complex/platform/access/dto/PermissionTemplateRequest.java`
- `backend/src/main/java/com/complex/platform/access/dto/PermissionTemplateApplyRequest.java`
- `backend/src/main/java/com/complex/platform/access/dto/TemporaryGrantRequest.java`
- `backend/src/main/java/com/complex/platform/access/dto/StatusChangeRequest.java`
- `backend/src/main/java/com/complex/platform/access/dto/RoleRequest.java`
- `backend/src/main/java/com/complex/platform/access/dto/AccessUserRequest.java`

### 修改后端文件

- `backend/src/main/java/com/complex/platform/common/api/ApiException.java`
  - 新增 `forbidden(String code, String message)`。
- `backend/src/main/java/com/complex/platform/auth/BearerAuthenticationFilter.java`
  - 保留当前认证，但认证主体必须可被 `AuthorizationService` 读取。
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessController.java`
  - 在 3 个 operation 入口调用 `AuthorizationService.requirePermission(...)`。
- `backend/src/main/java/com/complex/platform/evidence/AuditEventService.java`
  - 尽量复用现有成功/失败事件方法；如需 actor_id，新增最小重载，不破坏现有测试。

### 新增测试文件

- `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java`
- `backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java`
- `backend/src/test/java/com/complex/platform/access/AccessControllerTests.java`
- `backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java`

## 3. 数据结构变化

新增 `V003__p0_access_permission_authz_slice.sql`，只做向前兼容迁移。

- `permissions`
  - `permission_code varchar(128) primary key`
  - `permission_name varchar(128) not null`
  - `domain varchar(64) not null`
  - `risk_level varchar(32) not null`
  - `requires_approval boolean not null default false`
  - `status varchar(32) not null default 'active'`
  - `created_at timestamptz not null default now()`
- `access_users`
  - `id varchar(64) primary key`
  - `username varchar(64) not null unique`
  - `account_type varchar(32) not null`
  - `organization_id varchar(64)`
  - `merchant_id varchar(64)`
  - `status varchar(32) not null`
  - `created_at timestamptz not null`
- `roles`
  - `id varchar(64) primary key`
  - `role_name varchar(120) not null unique`
  - `description text`
  - `data_scope varchar(64) not null default 'project'`
  - `status varchar(32) not null default 'active'`
  - `created_at timestamptz not null`
- `role_permissions`
  - `role_id varchar(64) not null references roles(id)`
  - `permission_code varchar(128) not null references permissions(permission_code)`
  - primary key `(role_id, permission_code)`
- `user_roles`
  - `user_id varchar(64) not null references access_users(id)`
  - `role_id varchar(64) not null references roles(id)`
  - primary key `(user_id, role_id)`
- 补强已有 `permission_templates`
  - `template_code`、`template_type`、`source_template_id`、`owner_scope_type`、`owner_scope_id`、`usage_mode`、`valid_from`、`valid_to`、`single_use`、`status`、`reason`、`approved_by`、`approved_at`、`updated_at`
- 新建或迁移 `permission_template_items`
  - 支持 `effect=grant/deny`、数据范围和敏感标记。
- 新建 `permission_template_applications`
  - 支持应用对象、有效期、单次有效、消耗、撤销和状态。
- 新建 `permission_template_usage_logs`
  - 记录授权计算结果，特别是 deny、expired、consumed。
- 补强已有 `temporary_permission_grants`
  - 增加 `status`、`valid_from`、`revoked_by`、`revoked_at`、`single_use`。
- 新建 `user_permission_overrides`
  - 支持员工级永久 grant/deny。

## 4. TDD 任务

### Task 1: 权限计算服务

**Files:**
- Create: `backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java`
- Create: `backend/src/main/java/com/complex/platform/access/AuthorizationService.java`
- Create: `backend/src/main/java/com/complex/platform/access/AccessActor.java`
- Modify: `backend/src/main/java/com/complex/platform/common/api/ApiException.java`

- [ ] **Step 1: 写失败测试**

测试必须覆盖：
- 没有权限时 `requirePermission("ops.production_readiness.manage")` 抛 `PERMISSION_DENIED`，HTTP 403。
- 同一权限同时存在 grant 和 deny 时按 deny。
- 临时模板过期后不生效。
- 单次模板第一次授权成功后消耗，第二次同一权限不再生效。
- local/dev/test 的 seed 管理员只允许使用明确预置的 `platform_admin` 全权限桥接，不允许任意 staff 自动全放行。

- [ ] **Step 2: 运行并确认失败**

Run: `npm run test:backend`

Expected: 新增测试因 `AuthorizationService` 或权限计算缺失失败。

- [ ] **Step 3: 最小实现**

实现 `AuthorizationService`，从 Spring SecurityContext 读取 `AuthService.UserRecord`，按 repository 返回的权限集合计算最终授权；deny 优先；失败抛 `ApiException.forbidden("PERMISSION_DENIED", "...")`。

- [ ] **Step 4: 运行并确认通过**

Run: `npm run test:backend`

Expected: 后端测试通过。

### Task 2: Flyway 权限表和 JDBC Repository

**Files:**
- Create: `backend/src/main/resources/db/migration/V003__p0_access_permission_authz_slice.sql`
- Create: `backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java`
- Create: `backend/src/main/java/com/complex/platform/access/AccessRepository.java`
- Create: `backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java`

- [ ] **Step 1: 写失败集成测试**

测试必须覆盖：
- 空库迁移后权限表存在。
- `permissions` 至少预置 access 与 ops 两组本切片需要的权限点。
- 创建模板时保存 grant/deny 明细。
- 复制模板保存快照，源模板后续变更不影响副本。
- 应用临时模板后，按有效期能查到最终权限。

- [ ] **Step 2: 运行并确认失败**

Run: `npm run test:backend`

Expected: 因 V003 和 JDBC Repository 缺失失败。

- [ ] **Step 3: 最小实现**

新增 V003 和 JDBC Repository。所有 `timestamptz` 写入必须用 UTC `OffsetDateTime`，不得直接绑定 `Instant`。

- [ ] **Step 4: 运行并确认通过**

Run: `npm run test:backend`

Expected: 后端测试通过。

### Task 3: Access API 契约实现

**Files:**
- Create: `backend/src/test/java/com/complex/platform/access/AccessControllerTests.java`
- Create: `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java`
- Create: `backend/src/main/java/com/complex/platform/access/AccessController.java`
- Create: `backend/src/main/java/com/complex/platform/access/AccessService.java`
- Create DTO files under `backend/src/main/java/com/complex/platform/access/dto/`

- [ ] **Step 1: 写失败测试**

测试必须覆盖：
- 未认证访问 access 接口返回 401/403。
- 没有对应权限访问 `POST /api/v1/access/permission-templates` 返回 `PERMISSION_DENIED`。
- `GET /api/v1/access/permissions` 返回统一 `ApiResponse<PageResponse<...>>`。
- 创建权限模板缺 `idempotency_key` 返回 `VALIDATION_FAILED`。
- 创建临时模板缺 `valid_until` 返回 `VALIDATION_FAILED`。
- `copyPermissionTemplate` 后副本权限不随源模板改变。
- `applyPermissionTemplate` 可批量应用到用户，写 `permission_template.applied` 事件。
- `createTemporaryGrant` 与 `revokeTemporaryGrant` 均写审计事件。

- [ ] **Step 2: 运行并确认失败**

Run: `npm run test:backend`

Expected: 因 Controller/Service 缺失或权限校验缺失失败。

- [ ] **Step 3: 最小实现**

实现 10 个 access operation 和 2 个用户 operation。写接口必须经过 `IdempotencyService`，成功/失败都写 `AuditEventService`。

- [ ] **Step 4: 运行并确认通过**

Run: `npm run test:backend`

Expected: 后端测试通过。

### Task 4: 生产前置接口接入操作级授权

**Files:**
- Modify: `backend/src/test/java/com/complex/platform/ops/productionreadiness/ProductionReadinessControllerTests.java`
- Modify: `backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessController.java`

- [ ] **Step 1: 写失败测试**

测试必须覆盖：
- 只有 `ops.production_readiness.view` 时，GET 成功，PUT 和 POST 检查失败。
- 只有 `ops.production_readiness.manage` 时，PUT 成功，GET 和 POST 检查失败。
- 只有 `ops.production_readiness.check` 时，POST 检查成功，GET 和 PUT 失败。

- [ ] **Step 2: 运行并确认失败**

Run: `npm run test:backend`

Expected: 当前 Controller 未调用授权服务，测试失败。

- [ ] **Step 3: 最小实现**

在 Controller 入口调用：
- GET: `authorizationService.requirePermission("ops.production_readiness.view")`
- PUT: `authorizationService.requirePermission("ops.production_readiness.manage")`
- POST checks: `authorizationService.requirePermission("ops.production_readiness.check")`

- [ ] **Step 4: 运行并确认通过**

Run: `npm run test:backend`

Expected: 后端测试通过。

## 5. 验收标准

- `npm run verify:backend-env` 通过。
- `npm run test:backend` 通过，新增测试必须实际经历红绿过程。
- `npm run verify:p0-contract` 通过，不新增旧路径。
- `/opt/complex/scripts/complex-verify-p0.sh` 通过。
- 未认证访问 access 和 ops 受保护接口返回 401/403。
- 认证但缺 operation 权限访问对应接口返回 403 + `PERMISSION_DENIED`。
- 有 operation 权限访问对应接口返回 200。
- 模板复制为权限快照；源模板后续变更不影响副本。
- 临时授权按 `valid_until` 失效；撤销后立即失效。
- 单次有效模板授权被消费后不再生效。
- 每次模板创建、修改、复制、应用、撤销、临时授权创建/撤销、权限拒绝均可在审计事件中追溯。

## 6. 安全检查点

- 不得把真实密码、密钥、数据库连接串写入代码、文档或测试输出。
- 不得把 `ROLE_P0_SMOKE` 当作所有接口的正式权限。
- 不得让 `authenticated` 等价于 `authorized`。
- 不得接受未知 `permission_code`；必须来自 `permissions` 字典。
- 不得在 OpenAPI 外新增旧 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 路径。
- 权限拒绝必须 fail-closed，不能因 repository 异常而放行。

## 7. 风险和回滚

- 风险：V003 表结构与 V002 最小权限表重叠。
  - 回滚：恢复开工前 tar 快照；如只需修正迁移，在开发库调整 V003 后重新迁移。
- 风险：操作级授权接入后 smoke 管理员没有种子权限导致 160 验证失败。
  - 回滚：不得改回“任意认证即放行”；应修正 seed 管理员权限初始化或测试配置。
- 风险：Controller 测试过度 mock，无法证明真实授权链路。
  - 回滚：补 `AccessJdbcIntegrationTests` 和 live API 验证；避免只断言 mock 调用。
- 风险：当前 160 非 Git 仓库。
  - 回滚：使用 `/home/james/complex-management-backups/p0-access-authz-before-*.tgz` 恢复。


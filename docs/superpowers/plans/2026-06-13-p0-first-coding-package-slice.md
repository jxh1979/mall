# P0 首批编码包第一实施切片 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 192.168.88.160 上用 Codex 正式实现 P0 首批编码包的第一条可验收垂直切片：生产前置台账 API + 审计事件 + 幂等记录 + 首批正式 Flyway 表骨架，并保持现有 P0 契约、后端测试和 160 smoke 不倒退。

**Architecture:** 以 `backend/` 为唯一正式后端入口，采用 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Service/Repository。旧 `apps/api`、Fastify、Prisma、SQLite 仅作为废止原型，不得参与实现。第一切片先落 `ops` 域和共用底座，不提前进入票务、酒吧、游乐交易主流程。

**Tech Stack:** Java 21, Spring Boot 4.1.0, Maven 3.9+, PostgreSQL, Flyway, Spring MVC, Spring Security, springdoc-openapi, JUnit 5, AssertJ.

---

## 1. 需求和边界审计结论

- 当前可直接开工：160 上 `/home/james/complex-management` 已具备 Java 21、Maven 3.9.11、Node 22、npm 10，`npm run verify:backend-env`、`npm run test:backend`、`npm run verify:p0-contract` 已在改动前通过。
- 当前远端源码目录不是 Git 仓库，正式编码前必须做目录快照，回滚以快照恢复为准。
- 正式后端只允许修改 `backend/`、`docs/openapi/`、`docs/p0-openapi-operation-acceptance-matrix-v0.1.md`、`docs/p0-openapi-contract-closure-appendix-v0.1.md` 和必要验证脚本；不得修改 `apps/api`。
- 生产前置台账模板的状态枚举为 `not_started`、`pending_info`、`in_progress`、`blocked`、`ready_for_test`、`passed`、`failed`、`waived`、`not_applicable`；OpenAPI 当前仍有较粗的 `pending`。本切片默认以台账模板为准，先修 OpenAPI 枚举，再实现代码。
- P0 高风险边界不变：域名 HTTPS 暂不绑定；真实支付、MDM、人脸年龄核验、NAS、企业微信、第三方 SDK/API 缺资料时只记录为生产前置台账阻断项，不阻塞编码和 mock，不得标记真实生产就绪。

## 2. 本切片范围

### 必做

- [ ] CP-BE-001：新增正式包结构占位，不写空业务逻辑：
  - `backend/src/main/java/com/complex/platform/ops/`
  - `backend/src/main/java/com/complex/platform/evidence/`
  - `backend/src/main/java/com/complex/platform/access/`
  - `backend/src/main/java/com/complex/platform/governance/`
  - `backend/src/main/java/com/complex/platform/finance/`
- [ ] CP-BE-002：补齐幂等、分页、统一错误和审计服务的最小可用底座。
- [ ] CP-DB-001：新增 `V002__p0_foundation_first_slice.sql`，创建首批正式表骨架。
- [ ] CP-EVT-001：实现 `audit_events` 写入服务，至少覆盖生产前置台账查询、更新、检查成功/失败事件。
- [ ] CP-OPS-001：实现 3 个 OpenAPI operation：
  - `GET /api/v1/ops/production-readiness/items`
  - `PUT /api/v1/ops/production-readiness/items/{id}`
  - `POST /api/v1/ops/production-readiness/checks`
- [ ] CP-QA-001：新增失败测试、实现、回归验证，并保持 `complex-verify-p0.sh` 可继续运行。

### 不做

- [ ] 不迁移票务、酒吧、游乐、库存、会员主流程。
- [ ] 不引入独立 Python AI 服务。
- [ ] 不接真实支付、真实 MDM、人脸年龄核验、NAS、企业微信或第三方供应商 SDK。
- [ ] 不把 `local/dev/test` smoke Controller 标记为生产实现完成。

## 3. 需要修改的文件

### 文档和契约

- `docs/openapi/openapi-p0.yaml`
- `docs/openapi/openapi-p0.json`
- `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`
- `docs/p0-openapi-contract-closure-appendix-v0.1.md`

### 后端新增

- `backend/src/main/resources/db/migration/V002__p0_foundation_first_slice.sql`
- `backend/src/main/java/com/complex/platform/common/api/PageResponse.java`
- `backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyRecord.java`
- `backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyRepository.java`
- `backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java`
- `backend/src/main/java/com/complex/platform/evidence/AuditEvent.java`
- `backend/src/main/java/com/complex/platform/evidence/AuditEventRepository.java`
- `backend/src/main/java/com/complex/platform/evidence/AuditEventService.java`
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessController.java`
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessService.java`
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessRepository.java`
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessItem.java`
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessCheck.java`
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessStatus.java`
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/dto/ProductionReadinessItemResponse.java`
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/dto/ProductionReadinessItemUpdateRequest.java`
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/dto/ProductionReadinessCheckRequest.java`
- `backend/src/main/java/com/complex/platform/ops/productionreadiness/dto/ProductionReadinessCheckResponse.java`

### 后端测试新增

- `backend/src/test/java/com/complex/platform/ops/productionreadiness/ProductionReadinessServiceTests.java`
- `backend/src/test/java/com/complex/platform/ops/productionreadiness/ProductionReadinessControllerTests.java`
- `backend/src/test/java/com/complex/platform/evidence/AuditEventServiceTests.java`

### 可能修改

- `backend/src/main/java/com/complex/platform/common/api/GlobalExceptionHandler.java`
- `backend/src/main/java/com/complex/platform/config/SecurityConfig.java`
- `backend/src/main/java/com/complex/platform/config/OpenApiConfig.java`

## 4. 数据结构变化

新增 Flyway 表必须使用 PostgreSQL 语法，不使用 Hibernate 自动建表。

- `audit_events`
  - `id uuid primary key`
  - `occurred_at timestamptz not null`
  - `event_name varchar(128) not null`
  - `event_result varchar(32) not null`
  - `actor_type varchar(64)`
  - `actor_id varchar(128)`
  - `auth_context varchar(64)`
  - `terminal varchar(64)`
  - `permission_code varchar(128)`
  - `business_type varchar(128)`
  - `business_id varchar(128)`
  - `request_id varchar(128)`
  - `idempotency_key varchar(128)`
  - `metadata jsonb not null default '{}'::jsonb`
  - index: `event_name, occurred_at desc`; `business_type, business_id`; `idempotency_key`
- `idempotency_records`
  - `id uuid primary key`
  - `operation_id varchar(128) not null`
  - `idempotency_key varchar(128) not null`
  - `request_hash varchar(128) not null`
  - `response_code varchar(64)`
  - `response_body jsonb`
  - `http_status integer`
  - `created_at timestamptz not null`
  - `expires_at timestamptz not null`
  - unique: `operation_id, idempotency_key`
- `production_readiness_items`
  - `id varchar(64) primary key`
  - `category varchar(64) not null`
  - `item_name varchar(256) not null`
  - `default_solution text not null`
  - `owner_department varchar(128) not null`
  - `owner_person varchar(128)`
  - `supplier_name varchar(128)`
  - `status varchar(64) not null`
  - `target_date date`
  - `evidence_link text`
  - `last_test_time timestamptz`
  - `blocking_scope text`
  - `fallback_mode text`
  - `credential_ref varchar(256)`
  - `runbook_ref varchar(256) not null`
  - `next_action text not null`
  - `updated_at timestamptz not null`
  - `updated_by varchar(128)`
- `production_readiness_checks`
  - `id uuid primary key`
  - `check_scope varchar(64) not null`
  - `result varchar(32) not null`
  - `blocker_count integer not null`
  - `operator_note text`
  - `checked_at timestamptz not null`
  - `checked_by varchar(128)`
  - `idempotency_key varchar(128)`
- `production_readiness_check_blockers`
  - `check_id uuid not null references production_readiness_checks(id)`
  - `item_id varchar(64) not null references production_readiness_items(id)`
  - `status varchar(64) not null`
  - `blocking_scope text`
  - primary key: `check_id, item_id`
- 权限、主体绑定、应收应付首批骨架表可以先建最小约束，不在本切片开放业务 API：
  - `permission_templates`
  - `permission_template_permissions`
  - `temporary_permission_grants`
  - `business_entity_bindings`
  - `finance_ar_ap_items`
  - `finance_ar_ap_payments`

## 5. 接口行为

### GET `/api/v1/ops/production-readiness/items`

- 认证上下文：`staff-token`
- 权限：`ops.production_readiness.view`
- 返回：`ApiResponse<PageResponse<ProductionReadinessItemResponse>>`
- 行为：
  - 返回默认 12 项生产前置台账。
  - 写入 `production_readiness.viewed` 成功事件。
  - 查询失败写入 `production_readiness.view_failed`。

### PUT `/api/v1/ops/production-readiness/items/{id}`

- 认证上下文：`staff-token`
- 权限：`ops.production_readiness.manage`
- 幂等：必须提供 `idempotency_key`
- 请求字段采用 OpenAPI `snake_case`
- 行为：
  - 不存在的 `id` 返回 404 或业务错误码 `PRODUCTION_READINESS_ITEM_NOT_FOUND`。
  - 明文密钥特征写入 `credential_ref` 时拒绝，错误码 `SECRET_VALUE_REJECTED`。
  - 重复 `idempotency_key` + 相同请求返回首次处理结果。
  - 重复 `idempotency_key` + 不同请求返回 409，错误码 `IDEMPOTENCY_CONFLICT`。
  - 成功写入 `production_readiness.updated`。
  - 失败写入 `production_readiness.update_failed`。

### POST `/api/v1/ops/production-readiness/checks`

- 认证上下文：`staff-token`
- 权限：`ops.production_readiness.check`
- 幂等：必须提供 `idempotency_key`
- 行为：
  - `check_scope=all` 汇总所有台账项。
  - `check_scope=mdm/payment/age_verification/nas_backup/credential_rotation/wecom/third_party/server_160/https_domain` 只检查对应类别。
  - 阻断状态：`not_started`、`pending_info`、`in_progress`、`blocked`、`ready_for_test`、`failed`。
  - 非阻断状态：`passed`、`waived`、`not_applicable`。
  - `https_domain` 当前允许只提示，不阻塞内网开发/编码验收；公开生产仍保留前置提醒。
  - 成功写入 `production_readiness.checked`。
  - 失败写入 `production_readiness.check_failed`。

## 6. TDD 执行步骤

### 6.1 开工快照

- [ ] 在 160 执行：
  - `cd /home/james`
  - `tar -czf complex-management-backups/p0-first-slice-before-$(date +%Y%m%d-%H%M%S).tgz complex-management`

### 6.2 契约枚举先红后绿

- [ ] 先新增或调整契约校验期望，使 `ProductionReadinessItemUpdateRequest.status` 必须包含台账模板 9 个状态并不再使用 `pending`。
- [ ] 运行 `npm run verify:p0-contract`，确认失败。
- [ ] 修改 `docs/openapi/openapi-p0.yaml` 与 `docs/openapi/openapi-p0.json`。
- [ ] 运行：
  - `npm run docs:operation-matrix`
  - `npm run docs:openapi-closure`
  - `npm run verify:p0-contract`
- [ ] 期望：契约恢复通过。

### 6.3 服务行为测试先红后绿

- [ ] 新增 `ProductionReadinessServiceTests`，覆盖：
  - 默认 12 项可查询。
  - 更新不存在项失败。
  - `credential_ref` 明文密钥拒绝。
  - 阻断项未通过时检查结果为 `blocked`。
  - 全部关键项 `passed/waived/not_applicable` 时检查结果为 `passed`。
  - 重复幂等键相同请求返回相同结果。
  - 重复幂等键不同请求返回冲突。
- [ ] 运行 `npm run test:backend`，确认测试失败。
- [ ] 实现最小 Service/Repository/DTO。
- [ ] 运行 `npm run test:backend`，确认通过。

### 6.4 Controller 测试先红后绿

- [ ] 新增 `ProductionReadinessControllerTests`，覆盖：
  - 未认证访问返回 401/403。
  - 查询接口返回统一 `ApiResponse` 和分页结构。
  - 更新接口缺 `idempotency_key` 返回 `VALIDATION_FAILED`。
  - 检查接口成功返回阻断项数量。
- [ ] 运行 `npm run test:backend`，确认失败。
- [ ] 实现 Controller 和必要异常映射。
- [ ] 运行 `npm run test:backend`，确认通过。

### 6.5 审计事件测试先红后绿

- [ ] 新增 `AuditEventServiceTests`，覆盖成功/失败事件写入模型，不测试 mock 本身。
- [ ] 运行 `npm run test:backend`，确认失败。
- [ ] 实现 `AuditEventService`。
- [ ] 运行 `npm run test:backend`，确认通过。

### 6.6 Flyway 验证

- [ ] 新增 `V002__p0_foundation_first_slice.sql`。
- [ ] 运行 `npm run test:backend`，确认编译通过。
- [ ] 运行 `npm run verify:backend-env`。
- [ ] 通过 160 发布/验证脚本确认 Flyway 在 PostgreSQL 上可执行。

## 7. 验收标准

- [ ] `npm run verify:backend-env` 通过。
- [ ] `npm run test:backend` 通过，新增测试实际经历红绿过程。
- [ ] `npm run verify:p0-contract` 通过。
- [ ] `/api/v1/ops/production-readiness/items`、`/{id}`、`/checks` 与 OpenAPI operationId、权限、幂等、事件一致。
- [ ] `credential_ref` 不接受疑似明文密钥。
- [ ] 生产前置检查能明确返回阻断项和阻断范围。
- [ ] 160 `ops/160/complex-verify-p0.sh` 通过或给出明确外部阻断原因。
- [ ] 不修改 `apps/api`，不恢复 TypeScript/SQLite 原型。

## 8. 风险和回滚方式

- 风险：远端目录不是 Git 仓库，无法用 Git 直接回滚。
  - 回滚：使用开工前 `complex-management-backups/p0-first-slice-before-*.tgz` 恢复 `/home/james/complex-management`。
- 风险：OpenAPI 枚举改动导致矩阵/追溯附录漂移。
  - 回滚：恢复 OpenAPI JSON/YAML 并重新运行 `npm run docs:operation-matrix`、`npm run docs:openapi-closure`。
- 风险：Flyway V002 在已有 160 数据库上失败。
  - 回滚：不得手工修改生产表；先停在开发库，修正迁移；若已进入测试库且失败，按 160 Runbook 恢复备份。
- 风险：新增正式 Controller 影响现有 smoke。
  - 回滚：恢复快照；或临时停用新 Controller，但不得把 smoke 标记为生产实现。
- 风险：真实密钥被误写入台账。
  - 回滚：立即删除记录、轮换对应密钥、写入安全事件；代码层必须先做明文密钥特征拒绝。

## 9. 远端 Codex 执行要求

- 在 160 上执行：
  - `cd /home/james/complex-management`
  - `/home/james/.local/bin/codex exec --skip-git-repo-check --dangerously-bypass-approvals-and-sandbox -C /home/james/complex-management -`
- 给远端 Codex 的提示必须包含：
  - 使用中文。
  - 严格按本计划逐项执行。
  - 先写失败测试并展示失败，再写最小实现。
  - 不修改 `apps/api`。
  - 不写真实账号、密钥、数据库密码。
  - 每完成一个小节运行对应验证命令。
  - 最终输出修改文件、测试结果、安全风险和未完成项。

## 10. 本机质量检查清单

- [ ] 检查远端 Codex 最终报告是否列出红绿测试证据。
- [ ] 抽查远端变更文件是否只落在计划范围。
- [ ] 运行远端：
  - `npm run test:backend`
  - `npm run verify:p0-contract`
  - `ops/160/complex-verify-p0.sh`
- [ ] 使用 codex-security 或等效安全审计检查：
  - 明文密钥拒绝。
  - 鉴权和权限声明未弱化。
  - 幂等冲突不能重复写入状态。
  - 审计事件成功/失败均留痕。
  - OpenAPI 与实现未出现旧路径回退。

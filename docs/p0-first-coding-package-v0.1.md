# P0 首批编码包 V0.1

> 创建日期：2026-06-18（补齐：当前入口 `00-current-effective-document-entry-v0.1.md` 在 S1 事实源、阅读顺序与多角色最小阅读包中 13 处引用本文件，但此前文件缺失，造成 S1 编码入口断链。本文件按入口对它的定义补齐，并指向已实现的正式工件。）
> 状态：S1 编码入口包。首批范围**已实现并通过验收闸口**（见 §5），本文件作为首批边界、反模式与验收的权威入口与索引。
> 配套入口：`p0-coding-entry-pack-v0.1.md`（编码环境与契约入口）、`p0-implementation-task-list.md`（任务清单）、`p0-api-contract-pack-v0.1.md` + `openapi/openapi-p0.*`（接口契约）。

机器识别标记：P0_FIRST_CODING_PACKAGE_READY、FIRST_BATCH_SCOPE_AUTHORITATIVE。

## 1. 首批可编码范围（BE-001 / BE-002 / DB-001 / AUTH / 事件 / 主体绑定 / 应收应付 / 生产前置台账）

| 工作包 | 范围 | 已实现工件 |
|---|---|---|
| BE-001 平台地基 | 统一响应、异常处理、鉴权过滤、权限模板、幂等、审计事件执行器 | `common/api/*`、`access/AuthorizationService`、`common/idempotency/IdempotencyService`、`evidence/AuditedOperationExecutor` |
| BE-002 主数据/治理 | 经营/运营/合同/结算主体、商户、合同、主体绑定 | `governance/PlatformService`、`business-entity-binding-*`、迁移 V005 |
| DB-001 Flyway 首批迁移 | PostgreSQL + Flyway 正式迁移（非 SQLite 原型）| `backend/src/main/resources/db/migration/V001–V0xx` |
| AUTH | 登录/会话/Bearer、权限码、终端枚举 | `auth/*`、`access/*`、`unified-standards-baseline-v0.1.md` D1–D13 |
| 事件审计 | recordSuccess/Failure、business_type、idempotency 留痕 | `evidence/AuditEventService` + operation 矩阵 |
| 主体绑定 | 四类主体唯一绑定、变更留痕 | `governance` + `business-entity-binding-development-design-v0.1.md` |
| 应收应付 | AR/AP 台账、结算、对账（P0 完整台账）| `finance/FinanceService`、`finance-ar-ap-ledger-development-design-v0.1.md`、迁移 V044 |
| 生产前置台账 | 凭证/NAS/备份/恢复/告警台账与证据模板 | `production-readiness-implementation-ledger-template-v0.1.md`、`production-blocker-register-v0.1.md` |

详细任务与验收映射见 `p0-task-api-permission-event-acceptance-map-v0.1.md`、`p0-implementation-task-list.md`。

## 2. 接口与权限/事件口径

- 机器契约：`openapi/openapi-p0.json/yaml`（当前 170 paths / **207 operations**）。
- operation 级验收：`p0-openapi-operation-acceptance-matrix-v0.1.md`（含权限、终端、事件、示例、越权与数据范围必测）。
- 权限矩阵：`p0-operation-permission-matrix-v0.1.md`；事件证据：`p0-action-event-evidence-matrix-v0.1.md`。
- 数据隔离：`data-isolation-and-tenant-scope-security-spec-v0.1.md`（scope 注入只来自 token，越权必测）。

## 3. 强制反模式（不得违反）

- 不得按旧 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 或 TypeScript/Fastify/Prisma/SQLite 原型开发（已由 `verify:no-legacy-api-prototype`、`prototype-deprecation-and-technology-baseline-v0.1.md` 阻断）。
- 不得从请求参数读取 `merchant_id`/scope（见数据隔离规格 §2）。
- 不得把 AI 建议当作自动处罚/退款/核销/调价/绩效/合同状态依据（须人审兜底）。
- 不得把模板/本地 smoke 当作生产证据或生产就绪（可开发/联调 ≠ 生产上线）。
- 新增接口必须先改 OpenAPI → 生成 operation matrix/closure → 跑 `verify:p0-contract` → `verify:delivery-gates`。

## 4. 首批验收标准

- `verify:p0-contract`、`verify:delivery-gates` 通过。
- 后端单元/集成测试全绿；operation 矩阵与 OpenAPI 同步。
- 每个写操作有幂等键 + 审计事件 + 权限校验 + 数据范围过滤。
- 主体绑定唯一性、AR/AP 台账完整、生产前置台账区分 template/submitted/verified。

## 5. 首批实现当前态（实测，2026-06-18）

- 后端测试：**673 / 0 失败 / 0 错误 / BUILD SUCCESS**。
- OpenAPI：207 operations，operation matrix 与 closure appendix 同步。
- `verify:delivery-gates`：exit 0。
- 边界：当前为可开发/联调/业务验收态；**不可宣称生产上线**——外部生产证据（NAS/凭证/供应商）待 IT/Owner 回填，见 `production-blocker-register-v0.1.md`、`closed-loop-current-state-snapshot-v0.1.md`。

## 6. 下一步

首批包已实现并验收。后续按 `full-admin-srs-v0.1.md` + 页面/权限/字段矩阵推进完整后台；按 `third-party-supplier-integration-kit-v0.1.md` + 供应商占位（含短信，见 `third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md`）逐个接入真实第三方；生产上线前按 blocker register 回填外部证据。

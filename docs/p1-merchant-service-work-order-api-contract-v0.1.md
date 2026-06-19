# P1 商户服务工单 API 契约 V0.1

文档类型：Implementation / API Contract

本文档固化 P1-MERCHANT-SERVICE-01「商户服务/维修/投诉/整改工单」的最小可编码契约。它不替代 P0 OpenAPI 契约；P0 仍以 `docs/openapi/openapi-p0.yaml`、`docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 和 `docs/p0-openapi-contract-closure-appendix-v0.1.md` 为唯一事实源。

## 1. 范围

| 能力 | P1 本次包含 | 本次不包含 |
|---|---|---|
| 工单创建 | 商户服务、维修、投诉、整改四类工单 | 第三方客服系统自动同步 |
| 派单 | 指定责任部门、责任人，状态进入 `assigned` | 排班、自动派单、SLA 自动升级 |
| 处理 | 追加处理说明、证据引用，可变更到处理中/待商户/已解决 | 复杂协同子任务 |
| 关闭 | 记录解决方案、满意度、关闭时间和证据 | 商户重开、赔付审批 |
| 留痕 | 每次写操作记录业务动作和审计事件 | 附件上传、文件存储、病毒扫描 |

## 2. 状态机

| 状态 | 含义 | 允许动作 |
|---|---|---|
| `created` | 已创建未派单 | assign, action, close |
| `assigned` | 已派责任部门/人员 | action, close |
| `processing` | 处理中 | action, close |
| `pending_merchant` | 等商户确认或补充材料 | action, close |
| `resolved` | 已解决待关闭 | action, close |
| `closed` | 已关闭 | 不允许修改 |
| `cancelled` | 已取消 | 不允许修改 |

规则：`closed` 和 `cancelled` 为终态，后续不得派单、处理或关闭。

## 3. 接口

| Method | Path | Permission | 幂等 | 说明 |
|---|---|---|---|---|
| `GET` | `/api/v1/merchant-service/work-orders` | `merchant.service_work_order.view` | 否 | 查询工单列表 |
| `POST` | `/api/v1/merchant-service/work-orders` | `merchant.service_work_order.create` | 是 | 创建商户服务工单 |
| `POST` | `/api/v1/merchant-service/work-orders/{id}/assign` | `merchant.service_work_order.assign` | 是 | 派单 |
| `POST` | `/api/v1/merchant-service/work-orders/{id}/actions` | `merchant.service_work_order.process` | 是 | 追加处理动作 |
| `POST` | `/api/v1/merchant-service/work-orders/{id}/close` | `merchant.service_work_order.close` | 是 | 关闭工单 |

## 4. 字段

### 4.1 创建工单

| 字段 | 必填 | 类型 | 规则 |
|---|---|---|---|
| `idempotency_key` | 是 | string | 同一操作 24 小时内幂等 |
| `operator_note` | 否 | string | 为空时使用 `created` |
| `evidence_attachment_ids` | 否 | string[] | 仅存附件 ID 引用 |
| `merchant_id` | 是 | string | 关联商户 |
| `space_id` | 否 | string | 关联空间/铺位 |
| `contract_id` | 否 | string | 关联合同 |
| `work_order_type` | 是 | enum | `repair`/`complaint`/`rectification`/`service` |
| `priority` | 是 | enum | `low`/`medium`/`high`/`critical` |
| `title` | 是 | string | 1-160 字符 |
| `description` | 是 | string | 1-2000 字符 |
| `reported_by_user_id` | 是 | string | 上报人 |
| `reported_channel` | 是 | string | `web`、`staff_app`、`merchant_portal` 等 |
| `due_at` | 否 | instant | SLA 到期时间，UTC |

### 4.2 派单

| 字段 | 必填 | 类型 | 规则 |
|---|---|---|---|
| `idempotency_key` | 是 | string | 同一工单同一动作幂等 |
| `operator_note` | 是 | string | 派单说明 |
| `evidence_attachment_ids` | 否 | string[] | 证据引用 |
| `owner_department` | 是 | string | 责任部门 |
| `owner_user_id` | 是 | string | 责任人 |
| `operator_user_id` | 是 | string | 操作人 |

### 4.3 处理动作

| 字段 | 必填 | 类型 | 规则 |
|---|---|---|---|
| `idempotency_key` | 是 | string | 同一工单同一动作幂等 |
| `operator_note` | 是 | string | 处理说明 |
| `evidence_attachment_ids` | 否 | string[] | 证据引用 |
| `target_status` | 否 | enum | `assigned`/`processing`/`pending_merchant`/`resolved` |
| `operator_user_id` | 是 | string | 操作人 |

### 4.4 关闭工单

| 字段 | 必填 | 类型 | 规则 |
|---|---|---|---|
| `idempotency_key` | 是 | string | 同一工单关闭动作幂等 |
| `operator_note` | 否 | string | 为空时使用 `closed` |
| `evidence_attachment_ids` | 否 | string[] | 证据引用 |
| `resolution` | 是 | string | 解决方案 |
| `satisfaction_score` | 否 | integer | 1-5 |
| `operator_user_id` | 是 | string | 操作人 |

## 5. 事件

| 动作 | 成功事件 | 失败事件 |
|---|---|---|
| 查询 | `merchant_service.work_order.viewed` | 暂不单列 |
| 创建 | `merchant_service.work_order.created` | `merchant_service.work_order.create_failed` |
| 派单 | `merchant_service.work_order.assigned` | `merchant_service.work_order.assign_failed` |
| 处理 | `merchant_service.work_order.action_added` | `merchant_service.work_order.action_failed` |
| 关闭 | `merchant_service.work_order.closed` | `merchant_service.work_order.close_failed` |

## 6. 错误处理矩阵

| 错误 | 检测 | 响应码 | 业务码 | 处理 |
|---|---|---|---|---|
| 缺少幂等键 | 写接口 `idempotency_key` 为空 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 必填字段为空 | 业务字段为空白 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 枚举非法 | 类型、优先级、目标状态非法 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 工单不存在 | ID 查不到 | 404 | `WORK_ORDER_NOT_FOUND` | 拒绝动作 |
| 终态后修改 | `closed`/`cancelled` 后继续派单/处理/关闭 | 409 | `WORK_ORDER_CLOSED` | 拒绝动作 |
| 幂等冲突 | 同 key 不同请求体 | 409 | `IDEMPOTENCY_CONFLICT` | 要求换 key 或查原请求 |

## 7. Anti-Patterns

| Don't | Do Instead | Why |
|---|---|---|
| 把商户服务工单塞进平台治理合同 Controller | 独立 `merchantservice` 子域 | 避免商户档案和服务履约边界混乱 |
| 存附件内容 | 存 `evidence_attachment_ids` | 附件归档由证据系统负责 |
| 不写动作流水，只改主表状态 | 主表 + action ledger 双写 | 运营复盘和审计需要过程 |
| 关闭后允许继续处理 | 终态拒绝修改 | 防止责任闭环被回写污染 |
| 无幂等直接创建/派单 | 写接口必须带幂等键 | 防重复提交 |
| 现在接入第三方客服 SDK | 保留内部 API，供应商到位后再映射 | 降低 P1 实施成本和外部依赖 |

## 8. Test Case Specifications

| Test ID | 类型 | 验收点 |
|---|---|---|
| MS-UT-001 | Unit | 创建工单缺商户 ID 返回 `VALIDATION_FAILED` 并写失败审计 |
| MS-UT-002 | Unit | 创建工单写主表记录、`created` 动作和成功审计 |
| MS-UT-003 | Unit | 派单写责任部门/责任人并追加 `assigned` 动作 |
| MS-UT-004 | Unit | 关闭后再次处理返回 `WORK_ORDER_CLOSED` |
| MS-UT-005 | Unit | 关闭必须有解决方案，满意度只能 1-5 |
| MS-UT-006 | Unit | 同幂等键同请求返回首次结果，不重复写业务动作和审计 |
| MS-IT-001 | Integration | V022 创建两张表和五个权限点 |
| MS-IT-002 | Integration | JDBC 能保存、查询工单和动作 |
| MS-IT-003 | Integration | P0 契约校验继续通过，P1 不漂移 P0 OpenAPI |

## 9. References

| Topic | Location |
|---|---|
| P1 总路线 | `docs/master-roadmap.md#阶段-8-市场基准增强与-p1p2-演进` |
| P0/P1 用户故事分层 | `docs/p0-user-story-map-and-sprint-slices-v0.1.md#第二期--p1` |
| 商户服务工单来源决策 | `docs/business-decision-log-v0.1.md#33-市场基准复核裁决` |
| P0 合同与主体边界 | `docs/platform-governance-product-spec-v0.2.md` |
| 本切片计划 | `docs/superpowers/plans/2026-06-14-p1-merchant-service-work-orders.md` |

## 后端开发设计补齐（P1）`R6 全闭环(2026-06-19)`

> 收口背景：本文件 §1-§9 仅固化了 API 契约层（请求/响应/状态/事件/测试用例），REQ-93 自标"后端未落地"——缺 `work_orders` 物理表结构、责任人/SLA 落库字段、派单/处理/关闭动作的服务端落地设计，以及与综合体统一异常链（F2）的衔接。本章在不改动 §1-§9 既有契约的前提下补齐后端开发设计，使契约可直接进入 Java 后端编码。物业工单（报修/投诉/整改/巡检）与 §1-§9 的商户服务工单同源同表，巡检类型为本章新增，状态 `done` 对应契约 §2 的 `resolved`（见下文映射）。

### A.1 work_orders 主表

承载 §2 状态机与 §4 字段，落库表 `work_orders`（与 §7 Anti-Patterns 一致，独立 `merchantservice` 子域，不并入平台治理合同表）。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | uuid | 主键 |
| `work_order_no` | varchar | 工单号，业务可读，平台内唯一 |
| `merchant_id` | uuid | 关联商户（必填，承接 §4.1） |
| `space_id` | uuid | 关联空间/铺位，可空 |
| `contract_id` | uuid | 关联合同，可空 |
| `work_order_type` | varchar | `repair`（报修）/`complaint`（投诉）/`rectification`（整改）/`inspection`（巡检）/`service`（商户服务），CHECK 约束；`inspection` 为物业巡检工单（R6 新增，承接业务侧"巡检"工单类型） |
| `priority` | varchar | `low`/`medium`/`high`/`critical` |
| `status` | varchar | `created`/`assigned`/`processing`/`pending_merchant`/`resolved`/`closed`/`cancelled`，CHECK 约束（与 §2 一致） |
| `title` | varchar(160) | 标题 |
| `description` | text | 描述 |
| `owner_department` | varchar | 责任部门，派单后写入 |
| `owner_user_id` | uuid | 责任人，派单后写入 |
| `reported_by_user_id` | uuid | 上报人 |
| `reported_channel` | varchar | 来源渠道 |
| `due_at` | timestamptz | SLA 到期时间（承接 §4.1 `due_at`） |
| `sla_breached` | boolean | SLA 是否已超时，默认 false；处理/关闭或定时巡检时回写 |
| `sla_breached_at` | timestamptz | 实际判定超时的时间，可空 |
| `resolution` | text | 解决方案，关闭时写入 |
| `satisfaction_score` | smallint | 满意度 1-5，可空 |
| `assigned_at` | timestamptz | 派单时间，可空 |
| `resolved_at` | timestamptz | 解决时间（状态进入 `resolved` 时回写，对应业务"完成 done"），可空 |
| `closed_at` | timestamptz | 关闭时间，可空 |
| `evidence_attachment_ids` | jsonb | 证据附件 ID 引用（仅引用，不存内容，与 §7 一致） |
| `created_at` | timestamptz | 创建时间 |
| `updated_at` | timestamptz | 更新时间 |

索引：`idx_work_orders_merchant_status`、`idx_work_orders_owner_user`、`idx_work_orders_due_at`、`uq_work_orders_no`（唯一）。

> 状态映射说明：业务侧口径"创建 created → 派单 assigned → 处理 processing → 完成 done → 关闭 closed"中的 **`done` = 契约 §2 的 `resolved`**（已解决待关闭）；`pending_merchant`（等商户）为商户服务/投诉类的中间态，巡检/报修类工单可不经过 `pending_merchant` 直接 `processing → resolved`。`done/已完成` 仅作 UI 文案，落库统一用 `resolved`，避免与 §2 状态机分叉。

### A.2 work_order_actions 动作流水表（action ledger）

落地 §7「主表 + action ledger 双写」，承载每次派单/处理/关闭的过程留痕。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | uuid | 主键 |
| `work_order_id` | uuid | 工单 ID |
| `action_type` | varchar | `created`/`assigned`/`action`/`closed`/`cancelled` |
| `from_status` | varchar | 动作前状态，可空 |
| `to_status` | varchar | 动作后状态 |
| `operator_user_id` | uuid | 操作人 |
| `operator_note` | text | 操作说明 |
| `evidence_attachment_ids` | jsonb | 本次动作证据引用 |
| `idempotency_key` | varchar | 幂等键（承接 §4 各动作幂等） |
| `created_at` | timestamptz | 动作时间 |

唯一约束：`uq_work_order_actions_idem(work_order_id, action_type, idempotency_key)`，落地 §6「幂等冲突」检测。

### A.3 API 后端落地（与 §3 契约对齐）

§3 五个端点的服务端行为：

| 动作 | 端点（§3） | 后端落地 |
|---|---|---|
| 创建 | `POST .../work-orders` | 校验 §6 必填/枚举 → 生成 `work_order_no`、`status=created` → 写主表 + `created` 动作 → 发 `merchant_service.work_order.created` |
| 派单 | `POST .../{id}/assign` | 校验非终态 → 写 `owner_department`/`owner_user_id`/`assigned_at`、`status=assigned` → 写 `assigned` 动作 → 发 `merchant_service.work_order.assigned` |
| 处理 | `POST .../{id}/actions` | 校验 `target_status` 合法迁移 → 流转到 `processing`/`pending_merchant`/`resolved`（`resolved` 回写 `resolved_at`）→ 写 `action` 动作 → 发 `merchant_service.work_order.action_added` |
| 关闭 | `POST .../{id}/close` | 校验有 `resolution`、满意度 1-5 → `status=closed`、写 `closed_at` → 写 `closed` 动作 → 发 `merchant_service.work_order.closed` |
| 查询 | `GET .../work-orders` | 列表查询，按商户/状态/责任人/SLA 过滤 |

SLA 落地：创建/派单时按 `priority` 与业务配置写 `due_at`；处理/关闭或后台定时任务比对 `now > due_at` 且未到终态时置 `sla_breached=true`、`sla_breached_at=now`，并可发失败链事件提示升级（升级本身仍按 §1「不包含 SLA 自动升级」留 P1+，本期仅打标）。

### A.4 事件与 F2 统一异常链衔接

- 成功/失败事件沿用 §5 既有命名（`merchant_service.work_order.*`），不另起名。
- **与 F2 综合体统一异常链衔接**：报修/投诉/整改/巡检工单作为异常处置的承接载体之一，当工单由现场异常上报触发时，工单创建动作需携带来源异常单引用（`source_exception_id`，落在 `work_order_actions.evidence_attachment_ids` 同级的扩展或主表 `description` 结构化字段），并复用统一异常链的「第一处理人接单 → 处置 → 闭环」事件语义：工单 `assigned` 对应异常链接单、`resolved/closed` 对应异常闭环。异常链权威事件命名以 `operation-traceability-and-terminal-switch-prd §4.1` 为准，本工单域事件作为其下游消费方，不重复定义异常链事件名。

### A.5 验收

| 项 | 验收点 |
|---|---|
| 表落地 | `work_orders`、`work_order_actions` 两表迁移建成，含 CHECK 约束与唯一索引 |
| 类型覆盖 | `work_order_type` 支持 `repair/complaint/rectification/inspection/service`，巡检工单可创建并流转 |
| 状态闭环 | created→assigned→processing→resolved(=done)→closed 全链可走通，终态拒绝再修改（复用 §6/MS-UT-004） |
| 责任人/SLA | 派单写责任部门/责任人；`due_at` 超时置 `sla_breached` 并留痕时间 |
| 双写留痕 | 每次写操作主表 + `work_order_actions` 双写，幂等键去重（复用 MS-UT-006） |
| F2 衔接 | 由异常上报触发的工单可回溯来源异常单，工单闭环回写异常链闭环语义 |
| 不漂移 P0 | P1 工单后端不改动 P0 OpenAPI 契约（复用 MS-IT-003） |

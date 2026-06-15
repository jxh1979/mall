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

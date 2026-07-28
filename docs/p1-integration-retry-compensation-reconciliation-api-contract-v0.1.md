# P1 第三方回流、重试、补偿和对账接口合同 V0.1

## 1. 范围

本合同用于 P1 阶段把第三方餐饮、零售、券核销等回流数据先落成可验证的后台骨架。供应商 SDK/API 未最终确定前，系统不写死任何供应商私有字段；私有字段只能进入 `provider_payload` 或 `payload` JSON。

不包含真实供应商 SDK 调用、不包含定时调度器、不直接改写票务、酒吧、财务或库存主表。

## 2. 权限

| 权限 | 用途 |
| --- | --- |
| `integration.order_sync.view` | 查看第三方订单/退款/券核销回流记录 |
| `integration.order_sync.manage` | 创建第三方回流记录 |
| `integration.retry.view` | 查看重试任务 |
| `integration.retry.manage` | 创建重试任务、手动立即重试 |
| `integration.compensation.view` | 查看补偿任务 |
| `integration.compensation.manage` | 创建和处理补偿任务 |
| `integration.reconciliation.view` | 查看对账批次和差异 |
| `integration.reconciliation.manage` | 创建对账批次和差异 |
| `integration.reconciliation.resolve` | 处理对账差异 |

所有写接口必须传 `idempotency_key`，所有写操作必须产生审计事件。

## 3. 接口

### 3.1 订单回流

`GET /api/v1/integration/order-syncs`

返回第三方订单、退款、券核销回流记录。

`POST /api/v1/integration/order-syncs`

```json
{
  "idempotency_key": "sync-001",
  "provider_code": "mock-provider",
  "business_type": "catering",
  "sync_type": "order",
  "external_business_id": "external-order-1",
  "status": "synced",
  "amount_cent": 128800,
  "currency": "CNY",
  "provider_payload": {
    "provider_private_status": "PAID"
  }
}
```

字段规则：
- `business_type` 仅支持 `catering`、`retail`。
- `sync_type` 仅支持 `order`、`refund`、`voucher_verification`。
- `status` 仅支持 `received`、`synced`、`failed`、`difference_found`。
- `currency` P1 仅支持 `CNY`。

幂等规则：同一 `provider_code + business_type + sync_type + external_business_id` 更新同一条业务记录，不重复产生主记录。

### 3.2 重试任务

`GET /api/v1/integration/retry-tasks`

`POST /api/v1/integration/retry-tasks`

```json
{
  "idempotency_key": "retry-001",
  "provider_code": "mock-provider",
  "task_type": "webhook_process",
  "business_type": "catering",
  "external_business_id": "external-order-1",
  "status": "open",
  "max_retry_count": 3,
  "next_retry_at": "2026-06-14T00:05:00Z",
  "payload": {
    "reason": "timeout"
  }
}
```

`POST /api/v1/integration/retry-tasks/{id}/run-now`

```json
{
  "idempotency_key": "retry-now-001",
  "action": "run_now",
  "reason": "manual retry after provider timeout"
}
```

立即重试不会真实调用供应商；P1 只把任务状态置为 `ready`，重试次数加 1，并写留痕。

### 3.3 补偿任务

`GET /api/v1/integration/compensation-tasks`

`POST /api/v1/integration/compensation-tasks`

请求体同重试任务，`task_type` 可为 `manual_adjustment`、`webhook_compensation`、`polling_compensation` 等运营自定义类型。

`POST /api/v1/integration/compensation-tasks/{id}/resolve`

```json
{
  "idempotency_key": "compensation-resolve-001",
  "operator_user_id": "finance_1",
  "status": "resolved",
  "resolution_note": "manual ledger adjusted",
  "evidence_attachment_ids": ["att-resolution"]
}
```

处理补偿必须记录处理人、处理结论和附件证据。

### 3.4 对账批次

`GET /api/v1/integration/reconciliation/batches`

`POST /api/v1/integration/reconciliation/batches`

```json
{
  "idempotency_key": "batch-001",
  "provider_code": "mock-provider",
  "business_type": "catering",
  "period_start": "2026-06-14",
  "period_end": "2026-06-14",
  "expected_count": 10,
  "actual_count": 9,
  "difference_count": 1,
  "total_difference_cent": 100
}
```

状态由数量和差异自动推导：无差异为 `matched`，有差异为 `difference_found`。

### 3.5 对账差异

`GET /api/v1/integration/reconciliation/differences`

`POST /api/v1/integration/reconciliation/differences`

> **2026-07-28 v7 审计 P0-E 修订（对账旁路手填封堵）**：本端点由「录入差异」收窄为「**登记差异**」。
> 依据业务 HTML 铁律 `RULE-FIN-SOURCE-01`（业务:1340「平台侧金额必须系统自动取数、不许手填」）与决议 `R6-D07`。
> 原契约允许请求体直接给 `internal_amount_cent`（平台侧）/ `provider_amount_cent`（第三方侧）/ `difference_amount_cent`，
> 等于给了一条绕过系统取数、凭空造差异金额的后门；财务域同类接口（`POST /api/v1/finance/reconciliation/differences`）
> 早已按 R6-D07 强制系统取数，两套口径不一致。现统一：
>
> - **三个金额字段全部废止**。请求体带其中任一（哪怕值正确）一律 `400 RECONCILIATION_MANUAL_AMOUNT_FORBIDDEN`
>   ——**显式拒绝而非静默忽略**，避免操作员误以为自己填的数生效了。
> - **第三方侧金额系统取数**：按 `(provider_code, business_type, external_business_id)` 聚合
>   `integration_order_syncs` 的回流实收（订单额 − 退款额，钳 0，只认 `synced` / `difference_found`）。
> - **无系统取数支撑一律拒绝**：该外部单号毫无回流 → `400 RECONCILIATION_NO_SYSTEM_TAKINGS`。
>   要登记这笔，先按正规通道把第三方回流落库（order-sync 摄入 / 渠道自动对账），而不是在这里手打数字。
> - **平台侧恒 0**：本域没有任何按外部单号可查的平台侧金额台账，人工登记只能表达
>   「第三方回流有这笔、平台侧无对应取数」。平台侧确有对应取数的差异，必须走
>   `POST /api/v1/integration/reconciliation/channel-auto`（系统两侧比对产出），不得手工登记。
> - **强制进复核**：登记结果 `status = needs_review`，并记 `registered_by_user_id`；
>   复核（resolve）时**登记人不得自审**，否则 `403 SELF_REVIEW_FORBIDDEN`（操作者取自会话，不采信请求体 `operator_user_id`）。
> - **金额不可改**：差异落库后，处置只更新状态 / 处理人 / 结论 / 证据，两侧金额与登记人永不被覆盖（DB upsert 层锁死）。

```json
{
  "idempotency_key": "difference-001",
  "batch_id": "batch_1",
  "provider_code": "mock-provider",
  "business_type": "catering",
  "external_business_id": "external-order-1",
  "difference_type": "amount_mismatch",
  "assigned_to_user_id": "finance_1",
  "provider_payload": {
    "provider_private_status": "DIFF"
  }
}
```

响应中的 `internal_amount_cent` / `provider_amount_cent` / `difference_amount_cent` 由系统取数落值；
`amount_source` 标注 `system`（渠道自动对账产出）或 `manual_registration`（人工登记，金额仍系统取数）。

`POST /api/v1/integration/reconciliation/differences/{id}/resolve`

```json
{
  "idempotency_key": "difference-resolve-001",
  "operator_user_id": "finance_1",
  "status": "resolved",
  "resolution_note": "confirmed platform amount",
  "evidence_attachment_ids": ["att-diff"]
}
```

差异创建时必须校验 `batch_id` 存在，并且批次的 `provider_code`、`business_type` 与差异一致。

差异处置（resolve）除既有校验外，还须通过**差人复核守卫**：人工登记的差异（`needs_review`）不允许登记人本人处置。

## 4. 审计事件

| 事件 | 触发点 |
| --- | --- |
| `integration.order_sync.recorded` | 创建或更新回流记录 |
| `integration.retry_task.created` | 创建重试任务 |
| `integration.retry_task.run_now` | 手动触发立即重试 |
| `integration.compensation_task.created` | 创建补偿任务 |
| `integration.compensation_task.resolved` | 处理补偿任务 |
| `integration.reconciliation_batch.created` | 创建对账批次 |
| `integration.reconciliation_difference.created` | 创建对账差异 |
| `integration.reconciliation_difference.resolved` | 处理对账差异 |

失败事件必须使用对应 `_failed` 后缀，并记录错误码。

## 5. 验收标准

- 缺少 `idempotency_key` 的写接口返回 `VALIDATION_FAILED`。
- 无权限访问时被权限服务拦截。
- 第三方私有字段只保存在 JSON payload 字段，不进入业务主模型固定列。
- 重试任务立即执行后 `retry_count` 增加 1，状态变为 `ready`。
- 补偿任务处理后必须有处理人、处理说明和状态。
- 对账差异必须关联已存在批次，且批次和差异的供应商、业务类型一致。
- V030 迁移创建所有表、索引和权限，并授权平台管理员角色。
- 聚焦测试 `IntegrationServiceTests`、`IntegrationControllerTests`、`IntegrationJdbcIntegrationTests` 通过。

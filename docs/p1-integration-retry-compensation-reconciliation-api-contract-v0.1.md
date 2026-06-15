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

```json
{
  "idempotency_key": "difference-001",
  "batch_id": "batch_1",
  "provider_code": "mock-provider",
  "business_type": "catering",
  "external_business_id": "external-order-1",
  "difference_type": "amount_mismatch",
  "internal_amount_cent": 128800,
  "provider_amount_cent": 128700,
  "difference_amount_cent": 100,
  "assigned_to_user_id": "finance_1",
  "provider_payload": {
    "provider_private_status": "DIFF"
  }
}
```

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

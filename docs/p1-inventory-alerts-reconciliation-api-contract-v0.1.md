# P1 库存预警与供应商对账 API 合同 V0.12

## 1. 定位与边界

本合同覆盖 P1 供应链库存增强的最小可编码能力：安全库存预警、缺货提醒、批次保质期预警和供应商对账留痕。

本模块不做自动补货预测、不自动生成采购单、不自动付款、不做复杂 BOM 成本核算、不做供应商评分和招采比价。预警只是风险提示，采购与财务动作仍走对应领域流程。

## 2. 权限

| 权限码 | 用途 |
| --- | --- |
| `inventory.stock.view` | 查看库存余额和批次 |
| `inventory.stock.batch.manage` | 记录和维护库存批次 |
| `inventory.risk_alert.view` | 查看库存风险预警 |
| `inventory.risk.scan` | 执行库存风险扫描 |
| `inventory.supplier.reconcile` | 查看和创建供应商对账记录 |

## 3. API

### 3.1 查询库存批次

`GET /api/v1/inventory/stock-batches`

权限：`inventory.stock.view`

返回 `inventory_stock_batches` 分页结果。

### 3.2 记录库存批次

`POST /api/v1/inventory/stock-batches`

权限：`inventory.stock.batch.manage`

请求：

```json
{
  "idempotency_key": "batch-create-001",
  "evidence_attachment_ids": ["att_batch"],
  "item_id": "item_1",
  "location_id": "warehouse_1",
  "batch_no": "BATCH-202606-001",
  "supplier_id": "supplier_1",
  "quantity": 12,
  "expire_at": "2026-07-01T00:00:00Z",
  "status": "active"
}
```

批次状态：`active`、`consumed`、`expired`、`archived`。

### 3.3 查询库存风险预警

`GET /api/v1/inventory/risk-alerts`

权限：`inventory.risk_alert.view`

预警类型：

- `safety_stock_low`：可用库存低于物品 `min_quantity`。
- `out_of_stock`：可用库存为 0。
- `expiry_warning`：活动批次在扫描阈值内到期。

### 3.4 执行库存风险扫描

`POST /api/v1/inventory/risk-alerts/scan`

权限：`inventory.risk.scan`

请求：

```json
{
  "idempotency_key": "risk-scan-20260614",
  "expiry_warning_days": 7
}
```

返回：

```json
{
  "code": "OK",
  "data": {
    "generated_count": 3,
    "alerts": [
      {
        "alert_type": "safety_stock_low",
        "severity": "medium",
        "item_id": "item_1",
        "location_id": "warehouse_1",
        "available_quantity": 2,
        "threshold_quantity": 5,
        "status": "open"
      }
    ]
  }
}
```

扫描规则：

- `available_quantity <= 0` 生成 `out_of_stock`，严重级别 `high`。
- `0 < available_quantity < min_quantity` 生成 `safety_stock_low`，严重级别 `medium`。
- `active` 批次在 `expiry_warning_days` 天内到期，生成 `expiry_warning`，严重级别 `high`。
- 同一 `idempotency_key` 重放返回同一结果，不重复生成。

### 3.5 查询供应商对账

`GET /api/v1/inventory/supplier-reconciliations`

权限：`inventory.supplier.reconcile`

### 3.6 创建供应商对账

`POST /api/v1/inventory/supplier-reconciliations`

权限：`inventory.supplier.reconcile`

请求：

```json
{
  "idempotency_key": "supplier-recon-202606",
  "evidence_attachment_ids": ["att_invoice", "att_bank_flow"],
  "supplier_id": "supplier_1",
  "period_start": "2026-06-01",
  "period_end": "2026-06-30",
  "expected_amount_cent": 100000,
  "actual_amount_cent": 88000,
  "status": "difference_found"
}
```

系统自动计算 `difference_amount_cent = actual_amount_cent - expected_amount_cent`。

状态：`pending`、`matched`、`difference_found`。

## 4. 留痕

- 批次创建：`inventory.stock_batch.create`
- 风险扫描：`inventory.risk.scan`
- 风险预警查询：`inventory.risk_alert.list`
- 供应商对账创建：`inventory.supplier_reconciliation.create`
- 供应商对账查询：`inventory.supplier_reconciliation.list`

失败事件使用对应 `*_failed` 或 `*_view_failed` 名称。

## 5. P1 不做内容

- 不自动生成采购申请。
- 不自动扣改库存。
- 不自动付款或登记应付。
- 不接供应商真实 API。
- 不做复杂 BOM、杯卖配方成本、自动补货预测。

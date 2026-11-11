# P1 商户账单门户 API 合同 V0.13

## 1. 定位与边界

商户账单门户是 P1 商户协作层，用于把 P0 财务应收应付台账中的商户应收事项呈现为账单，并支持商户确认、费用异议和催缴留痕。

本模块不替代 P0 `ar_ap_items`，不直接登记收付款，不改变财务主口径。收付款、核销、对账仍进入 P0 财务域；本模块只保存商户侧账单快照和协作状态。

## 2. 权限

| 权限码 | 用途 | 风险等级 |
| --- | --- | --- |
| `merchant.billing.view` | 查看商户账单列表 | 中 |
| `merchant.billing.manage` | 创建/维护商户账单 | 高 |
| `merchant.billing.confirm` | 确认商户账单 | 中 |
| `merchant.billing.dispute.submit` | 提交账单异议 | 中 |
| `merchant.billing.dunning.record` | 记录催缴动作 | 高 |

P1 后台先使用员工权限体系保护接口；真实商户登录态接入后，商户端通过 BFF 或商户认证层复用同一账单服务。

## 3. 状态

账单状态：

- `issued`：已出账，等待商户查看或确认。
- `viewed`：商户已查看，暂由后续商户端接入写入。
- `confirmed`：商户确认账单。
- `disputed`：商户提交异议。
- `overdue`：逾期，暂由后续定时任务或人工维护写入。
- `closed`：账单协作关闭，不再催缴或异议。

异议状态：

- `submitted`
- `accepted`
- `rejected`
- `withdrawn`

催缴状态：

- `sent`
- `failed`
- `scheduled`

## 4. 接口

### 4.1 查询商户账单

`GET /api/v1/merchant-billing/bills`

权限：`merchant.billing.view`

响应：

```json
{
  "code": "OK",
  "data": {
    "items": [
      {
        "id": "mbill_202606",
        "merchant_id": "merchant_1",
        "ar_ap_item_id": "arap_202606",
        "bill_no": "MBILL202606140001",
        "bill_period_start": "2026-06-01",
        "bill_period_end": "2026-06-30",
        "due_date": "2026-07-05",
        "total_amount_cent": 168800,
        "currency": "CNY",
        "status": "issued",
        "aging_days": 0,
        "source_type": "manual",
        "source_id": "billing_batch_202606",
        "lines": [
          {
            "id": "mbill_line_rent",
            "fee_type": "rent",
            "description": "June rent",
            "amount_cent": 128800,
            "currency": "CNY"
          }
        ]
      }
    ],
    "page": 1,
    "page_size": 50,
    "total": 1
  }
}
```

### 4.2 创建商户账单

`POST /api/v1/merchant-billing/bills`

权限：`merchant.billing.manage`

要求：

- 必须传 `idempotency_key`。
- `ar_ap_item_id` 必须引用 P0 应收台账中的 `receivable` 项。
- `merchant_id` 必须等于 AR/AP 的 `counterparty_id`。
- `total_amount_cent` 必须等于 AR/AP 的 `amount_cent`。
- `lines.amount_cent` 汇总必须等于 `total_amount_cent`。

请求：

```json
{
  "idempotency_key": "bill-create-202606",
  "merchant_id": "merchant_1",
  "ar_ap_item_id": "arap_202606",
  "bill_period_start": "2026-06-01",
  "bill_period_end": "2026-06-30",
  "due_date": "2026-07-05",
  "total_amount_cent": 168800,
  "source_type": "manual",
  "source_id": "billing_batch_202606",
  "lines": [
    {"fee_type": "rent", "description": "June rent", "amount_cent": 128800},
    {"fee_type": "cam", "description": "June CAM", "amount_cent": 40000}
  ]
}
```

费用类型枚举：`rent`、`property_fee`、`energy_fee`、`cam`、`deposit`、`other`。

### 4.3 确认账单

`POST /api/v1/merchant-billing/bills/{id}/confirm`

权限：`merchant.billing.confirm`

请求：

```json
{
  "idempotency_key": "bill-confirm-202606"
}
```

允许从 `issued`、`viewed`、`overdue` 确认为 `confirmed`。

### 4.4 提交账单异议

`POST /api/v1/merchant-billing/bills/{id}/disputes`

权限：`merchant.billing.dispute.submit`

请求：

```json
{
  "idempotency_key": "bill-dispute-202606",
  "evidence_attachment_ids": ["att_meter_photo"],
  "reason_code": "energy_amount_mismatch",
  "description": "Energy reading does not match meter photo"
}
```

提交后账单状态进入 `disputed`，异议进入 `submitted`。

### 4.5 记录催缴

`POST /api/v1/merchant-billing/bills/{id}/dunning-records`

权限：`merchant.billing.dunning.record`

请求：

```json
{
  "idempotency_key": "bill-dunning-202606-01",
  "dunning_channel": "wecom",
  "message_template": "rent_overdue_first_notice",
  "status": "sent",
  "sent_at": "2026-06-16T02:00:00Z"
}
```

催缴渠道枚举：`wecom`、`sms`、`email`、`manual`。

## 5. 留痕与异常

- 查询账单写入 `merchant_billing.bill.viewed` 审计事件。
- 创建账单写入 `merchant.billing.create`。
- 确认账单写入 `merchant.billing.confirm`。
- 提交异议写入 `merchant.billing.dispute.submit`。
- 记录催缴写入 `merchant.billing.dunning.record`。
- 确认、异议、催缴的操作人一律取登录人（确认成功事件的 `operator_user_id`、异议单与催缴记录的操作人），请求体不传操作人。
- 校验失败写入对应 `*_failed` 审计事件。

错误码：

- `VALIDATION_FAILED`：字段缺失、金额不一致、费用类型非法、AR/AP 非应收。
- `AR_AP_ITEM_NOT_FOUND`：引用的 AR/AP 项不存在。
- `MERCHANT_BILL_NOT_FOUND`：账单不存在。
- `BILL_NOT_CONFIRMABLE`：当前状态不可确认。
- `BILL_NOT_DISPUTABLE`：当前状态不可异议。
- `BILL_CLOSED`：关闭账单不可催缴。

## 6. P1 不做内容

- 不做真实商户账号体系。
- 不做自动出账批处理。
- 不做收付款核销。
- 不做电子发票、税务票据。
- 不做复杂费用规则引擎；复杂费用规则保留到后续 P1/P2 子切片。

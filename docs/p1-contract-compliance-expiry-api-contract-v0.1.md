# P1 合同关键日期与证照到期 API 合同 V0.1

## 1. 定位与边界

本合同覆盖 P1 治理增强的最小可编码能力：合同关键日期台账、证照/资质/保险到期台账、到期提醒扫描和阻断建议留痕。

本模块不做 OCR、电子签章、保险理赔、法务审查自动化，也不自动修改商户、合同、游乐项目、库存或财务状态。系统只生成 `blocking_suggested` 建议，真正阻断或放行仍由对应业务域和审批流执行。

AI 辅助识别只作为后续输入来源保留：可记录 `source_type=ai_suggestion` 和 `human_confirmed` 字段，但 AI 结果必须人工确认后才能进入正式台账。

## 2. 权限

| 权限码 | 用途 |
| --- | --- |
| `compliance.contract_key_date.view` | 查看合同关键日期 |
| `compliance.contract_key_date.manage` | 新增和维护合同关键日期 |
| `compliance.expiry_item.view` | 查看证照/资质/保险到期台账 |
| `compliance.expiry_item.manage` | 新增和维护证照/资质/保险到期台账 |
| `compliance.reminder.view` | 查看到期提醒 |
| `compliance.reminder.scan` | 执行到期扫描 |

## 3. API

### 3.1 查询合同关键日期

`GET /api/v1/compliance/contract-key-dates`

权限：`compliance.contract_key_date.view`

返回合同关键日期分页结果。

### 3.2 创建合同关键日期

`POST /api/v1/compliance/contract-key-dates`

权限：`compliance.contract_key_date.manage`

请求：

```json
{
  "idempotency_key": "contract-date-create",
  "operator_note": "record contract end date",
  "evidence_attachment_ids": ["att-contract"],
  "contract_id": "contract_1",
  "merchant_id": "merchant_1",
  "date_type": "contract_end",
  "title": "Contract end",
  "due_date": "2026-07-01",
  "reminder_days": [90, 30, 7],
  "status": "active",
  "owner_user_id": "staff_legal",
  "source_type": "manual",
  "human_confirmed": true
}
```

`date_type`：`contract_start`、`contract_end`、`renewal_notice`、`deposit_return`、`rent_free_end`、`custom`。

`source_type`：`manual`、`ai_suggestion`、`imported`。

### 3.3 查询证照/资质/保险到期台账

`GET /api/v1/compliance/expiry-items`

权限：`compliance.expiry_item.view`

### 3.4 创建证照/资质/保险到期台账

`POST /api/v1/compliance/expiry-items`

权限：`compliance.expiry_item.manage`

请求：

```json
{
  "idempotency_key": "expiry-item-create",
  "operator_note": "record alcohol license",
  "evidence_attachment_ids": ["att-license"],
  "subject_type": "merchant",
  "subject_id": "merchant_1",
  "merchant_id": "merchant_1",
  "item_type": "alcohol_license",
  "item_name": "Alcohol license",
  "document_no": "LIC-001",
  "issuing_authority": "Market Authority",
  "valid_from": "2025-06-01",
  "valid_to": "2026-06-10",
  "reminder_days": [60, 30, 7],
  "expiry_action": "block_operation",
  "status": "active",
  "owner_user_id": "staff_compliance",
  "source_type": "manual",
  "human_confirmed": true
}
```

`subject_type`：`merchant`、`contract`、`supplier`、`asset`、`amusement_device`。

`item_type`：`business_license`、`food_license`、`alcohol_license`、`fire_certificate`、`special_equipment`、`insurance`、`supplier_qualification`、`inspection_certificate`、`custom`。

`expiry_action`：`remind_only`、`block_operation`、`supervisor_approval`。

### 3.5 查询到期提醒

`GET /api/v1/compliance/reminders`

权限：`compliance.reminder.view`

### 3.6 执行到期扫描

`POST /api/v1/compliance/reminders/scan`

权限：`compliance.reminder.scan`

请求：

```json
{
  "idempotency_key": "scan-20260614",
  "operator_note": "daily scan",
  "scan_date": "2026-06-14",
  "default_lookahead_days": 30,
  "operator_user_id": "staff_ops"
}
```

扫描规则：

- 合同关键日期已过期时生成 `expired`；`date_type=contract_end` 给出 `blocking_suggested=true`。
- 合同关键日期处于提醒窗口内时生成 `upcoming`，默认不直接阻断。
- 证照/资质/保险已过期时生成 `expired`；`expiry_action != remind_only` 给出 `blocking_suggested=true`。
- 证照/资质/保险处于提醒窗口内时生成 `upcoming`；`block_operation` 或 `supervisor_approval` 给出阻断或审批建议。
- 同一 `idempotency_key` 重放返回同一扫描结果，不重复生成。

## 4. 留痕

- 合同关键日期创建：`compliance.contract_key_date.created`
- 合同关键日期查询：`compliance.contract_key_date.viewed`
- 到期事项创建：`compliance.expiry_item.created`
- 到期事项查询：`compliance.expiry_item.viewed`
- 到期提醒扫描：`compliance.reminder.scan_completed`
- 到期提醒查询：`compliance.reminder.viewed`

失败事件使用对应 `*_failed` 或 `*_view_failed` 名称。

## 5. P1 不做内容

- 不做 OCR 扫描件识别。
- 不做电子签章、合同范本管理和法务条款审查。
- 不做保险理赔材料闭环。
- 不接政府、保险公司或供应商真实核验证书接口。
- 不自动修改商户准入、合同生效、游乐开放、酒吧售酒或财务台账状态。

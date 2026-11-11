# P1 合同关键日期与证照到期 API 合同 V0.12

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
| `compliance.grace_period.request` | 商户申请证照宽限期（第十八批 E 段第 22 条） |

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
  "scan_date": "2026-06-14",
  "default_lookahead_days": 30,
  "operator_user_id": "staff_ops"
}
```

扫描规则：

- 合同关键日期已过期时生成 `expired`；`date_type=contract_end` 给出 `blocking_suggested=true`。
- 合同关键日期处于提醒窗口内时生成 `upcoming`，默认不直接阻断。
- 证照/资质/保险已过期时生成 `expired`；`expiry_action != remind_only` 给出 `blocking_suggested=true`。
- 证照/资质/保险在「有效期止减该证照类型的预警提前月数」当日及以后、有效期止当日及以前生成 `upcoming`（预警提前月数按类型配置，未配置按出厂 3 个月；不再用条目上的 `reminder_days` 与请求的 `default_lookahead_days`，后者只对合同关键日期生效）；`block_operation` 或 `supervisor_approval` 给出阻断或审批建议。
- 宽限期已批准、扫描日在宽限截止日（含）以内的证照，每个扫描日另生成一条 `grace`（`due_date`＝扫描日，`blocking_suggested=false`）。
- 提醒 `message` 为中文：「{名称}将于{日期}到期」「{名称}已于{日期}到期」「{名称}宽限期至{日期}，到期仍未更新将阻断」。
- 每日 08:00（北京时间）系统自动对全平台执行同一套扫描（prod 开启 `compliance.reminder.scheduled.enabled`），留痕 `compliance.reminder.scan`。
- 同一 `idempotency_key` 重放返回同一扫描结果，不重复生成。

### 3.7 按证照类型配置宽限期与预警提前量（第十八批 E 段第 22 条，PGM:531）

`GET /api/v1/compliance/item-type-configs`（权限 `compliance.expiry_item.view`）：九类各一行 `item_type`、`grace_days`、`grace_min_days`、`grace_max_days`、`warning_months`、`warning_min_months`、`warning_max_months`；预警未配置时 `warning_months`＝3、上下限为空。

`PUT /api/v1/compliance/item-type-configs/{item_type}`（权限 `compliance.expiry_item.manage`，须幂等键）：请求体 `idempotency_key` 加上述六项。平台范围闸在幂等之前，商户账号 403 `PLATFORM_SCOPE_REQUIRED`；类型不在九类、每组三项不同空同填、不满足 0 ≤ 下限 ≤ 值 ≤ 上限、`insurance` 填宽限一律 400 `VALIDATION_FAILED`。宽限期不预置出厂值，未配置的类型不能申请。

### 3.8 商户申请宽限期

`POST /api/v1/compliance/expiry-items/{id}/grace-requests`（权限 `compliance.grace_period.request`，须幂等键）：请求体 `idempotency_key`、`reason`。范围闸在幂等之前、fail-closed：无商户归属 403 `MERCHANT_SCOPE_REQUIRED`，条目不存在 404 `COMPLIANCE_EXPIRY_ITEM_NOT_FOUND`，别家商户 403 `CROSS_MERCHANT_FORBIDDEN`。未过有效期止 409 `GRACE_NOT_EXPIRED`；类型未配置宽限（含保险）409 `GRACE_PERIOD_NOT_CONFIGURED`；本次到期已获批 409 `GRACE_ALREADY_USED`；在审 409 `GRACE_REQUEST_PENDING`；已过有效期止＋宽限天数 409 `GRACE_WINDOW_PASSED`；被驳回的可再申请。宽限截止日申请时固化＝有效期止＋宽限天数，提交即进统一审批的最高权限策略 `compliance_grace_period`（动作 `compliance.expiry_item.grace`），批准置 `grace_status=approved`、驳回置 `rejected`。证照条目返回体加 `grace_status`、`grace_until`。

### 3.9 证照续期登记

`POST /api/v1/compliance/expiry-items/{id}/renewal`（权限 `compliance.expiry_item.manage`，须幂等键）：请求体 `idempotency_key`、`valid_to`（必填，须晚于原有效期止，否则 400）、`valid_from`、`document_no`、`evidence_attachment_ids`（不填沿用原值）。商户账号只能更新本商户条目（403 `CROSS_MERCHANT_FORBIDDEN`），闸在幂等之前。更新后清空宽限申请，新到期可重新申请。

### 3.10 到期阻断

准入闸 `qualification_abnormal` 判定：本商户存在 `status=active`、有效期止早于今天（北京日历日）且不在已批准宽限期内的证照即命中；待审、驳回、超过宽限截止日照旧命中。

## 4. 留痕

- 合同关键日期创建：`compliance.contract_key_date.create`
- 合同关键日期查询：`compliance.contract_key_date.list`
- 到期事项创建：`compliance.expiry_item.create`
- 到期事项查询：`compliance.expiry_item.list`
- 到期提醒扫描：`compliance.reminder.scan`
- 到期提醒查询：`compliance.reminder.list`
- 按证照类型配置：`compliance.item_type_config.saved`
- 宽限期申请：`compliance.grace.submitted`；最高权限审批结果回写：`compliance.grace.decided`
- 证照续期登记：`compliance.expiry_item.renewed`（记原有效期止与新有效期止）

失败事件使用对应 `*_failed` 或 `*_view_failed` 名称。

## 5. P1 不做内容

- 不做 OCR 扫描件识别。
- 不做电子签章、合同范本管理和法务条款审查。
- 不做保险理赔材料闭环。
- 不接政府、保险公司或供应商真实核验证书接口。
- 不自动修改商户准入、合同生效、游乐开放、酒吧售酒或财务台账状态。

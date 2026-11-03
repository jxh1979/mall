# P1 酒吧账单拆单/合单接口契约 V0.11

## 1. 范围

本切片补齐酒吧 SaaS 增强中的账单级拆单、合单、操作留痕和最小权限闭环。适用于客人要求分开支付、同桌多账单合并结账、前台误操作后需要保留证据的场景。

本期只处理账单金额级拆分和账单级合并，不处理菜品/酒水明细级拆分、已支付账单合并、跨酒吧合并、财务结算后的反向重算、配方成本重算和员工提成重算。

## 2. 接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/v1/bar/bills/{bill_id}/split` | `bar.bill.split_merge` | 将一张未支付、未结算账单按金额拆出一张子账单 |
| POST | `/api/v1/bar/bills/{bill_id}/merge` | `bar.bill.split_merge` | 将另一张未支付、未结算且同酒吧账单合入主账单 |
| GET | `/api/v1/bar/bills/{bill_id}/operations` | `bar.bill.split_merge` | 查询账单拆单/合单操作记录 |

## 3. 请求

拆单：

```json
{
  "idempotency_key": "split-bill-once",
  "split_amount_cent": 40000,
  "reason": "guest wants separate payment",
  "requested_by_user_id": "cashier_1",
  "evidence_attachment_ids": ["att_1"]
}
```

合单：

```json
{
  "idempotency_key": "merge-bill-once",
  "secondary_bill_id": "bill_2",
  "reason": "same table checkout together",
  "requested_by_user_id": "cashier_1",
  "evidence_attachment_ids": ["att_1"]
}
```

## 4. 业务规则

- 拆单金额必须大于 0 且小于原账单应付金额。
- 已支付、已结算、已合并的账单不得拆单。
- 合单双方必须属于同一酒吧。
- 合单双方必须均为未支付、未结算状态。
- 合单不得将账单合入自身。
- 拆单成功后，原账单应付金额减少，生成子账单；子账单保持未支付、未结算。
- 合单成功后，主账单金额增加，副账单金额清零，并标记为 `merged`。
- 所有拆单/合单必须写入 `bar_bill_operations`，记录操作类型、主账单、副账单/生成账单、金额、原因、操作人、证据附件、快照和幂等键。

## 5. 错误码

| 错误码 | 触发条件 |
| --- | --- |
| `BAR_BILL_NOT_FOUND` | 账单不存在 |
| `BAR_BILL_NOT_SPLITTABLE` | 账单状态不允许拆单 |
| `BAR_BILL_SPLIT_AMOUNT_INVALID` | 拆单金额非法 |
| `BAR_BILL_NOT_MERGEABLE` | 账单状态不允许合单 |
| `BAR_BILL_MERGE_SAME_BILL` | 合单目标为同一账单 |
| `BAR_BILL_MERGE_BAR_MISMATCH` | 主副账单不属于同一酒吧 |

## 6. 审计事件

| 动作 | 成功事件 | 失败事件 |
| --- | --- | --- |
| 拆单 | `bar.bill.split` | `bar.bill.split_failed` |
| 合单 | `bar.bill.merge` | `bar.bill.merge_failed` |

## 7. 验收

- 相同 `idempotency_key` 的拆单/合单重试不会重复生成账单或重复叠加金额。
- 拆单后可查询到原账单、子账单和对应操作记录。
- 合单后可查询到主账单金额变更、副账单 `merged` 状态和对应操作记录。
- 已支付、已结算、已合并、跨酒吧、自合并、金额非法等场景必须返回明确错误码。
- V033 必须创建 `bar_bill_operations`，并写入 `bar.bill.split_merge` 权限，默认授予 `role_platform_admin`。

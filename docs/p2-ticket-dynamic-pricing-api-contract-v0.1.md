# P2-C 票务动态定价规则容器 API 契约 V0.13

## 1. 定位

本契约定义 P2-C 动态定价能力。它用于运营沉淀、发布和模拟动态票价规则；草稿规则只参与模拟，已发布且匹配的规则会进入正式票务订单金额，并在订单中保留价格来源留痕。

## 2. 权限

| 权限码 | 用途 | 风险级别 |
| --- | --- | --- |
| `ticket.dynamic_pricing.view` | 查看动态定价规则 | medium |
| `ticket.dynamic_pricing.manage` | 创建/维护动态定价规则草稿 | high |
| `ticket.dynamic_pricing.publish` | 发布动态定价规则 | high |
| `ticket.dynamic_pricing.simulate` | 模拟动态定价结果 | medium |

## 3. 数据模型

### TicketDynamicPricingRuleRecord

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 规则 ID |
| `ticket_type_id` | string | 票种 ID |
| `rule_name` | string | 规则名称 |
| `priority` | integer | 优先级，数值越大越优先 |
| `effective_from` | date | 生效开始日期 |
| `effective_to` | date | 生效结束日期 |
| `sales_channel` | string/null | 销售渠道；为空表示不限渠道 |
| `adjustment_type` | string | `percent_delta`、`fixed_delta` 或 `fixed_price`（第十八批 AW 段 338-R2 起） |
| `adjustment_rate` | decimal/null | 百分比增减，适用于 `percent_delta` |
| `adjustment_amount_cent` | long/null | 固定增减金额，适用于 `fixed_delta`；`fixed_price` 时为固定价（必填、不小于 0） |
| `min_price_cent` | long/null | 最低保护价 |
| `max_price_cent` | long/null | 最高保护价 |
| `status` | string | `draft`、`published`、`disabled` |
| `condition_rule` | object | 动态价条件，可空（第十八批 BP 段第 359 条起）：只认每日时段 `time_from`、`time_to`（时:分，须同时填写且不相同，结束早于开始表示跨零点）与到访当天已售张数门槛 `day_sold_min`（不小于 1 的整数）；值为 null 视同未填，带其他键 400 `VALIDATION_FAILED` |
| `operator_note` | string/null | 操作说明 |
| `evidence_attachment_ids` | string[] | 证据附件 ID |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

## 4. 接口

### 4.1 查看规则

`GET /api/v1/ticketing/dynamic-pricing-rules`

- 权限：`ticket.dynamic_pricing.view`
- 响应：`PageResponse<TicketDynamicPricingRuleRecord>`

### 4.2 创建规则草稿

`POST /api/v1/ticketing/dynamic-pricing-rules`

- 权限：`ticket.dynamic_pricing.manage`
- 必填幂等键：`idempotency_key`
- 初始状态：`draft`

请求示例：

```json
{
  "idempotency_key": "dyn-create-001",
  "operator_note": "暑期周末晚场上浮",
  "evidence_attachment_ids": ["att_dynamic_001"],
  "ticket_type_id": "ticket_type_1",
  "rule_name": "Summer weekend uplift",
  "priority": 100,
  "effective_from": "2026-06-01",
  "effective_to": "2026-06-30",
  "sales_channel": "web",
  "adjustment_type": "percent_delta",
  "adjustment_rate": 20.00,
  "min_price_cent": 9900,
  "max_price_cent": 15000,
  "condition_rule": {
    "time_from": "20:00",
    "time_to": "22:00",
    "day_sold_min": 50
  }
}
```

### 4.3 发布规则

`POST /api/v1/ticketing/dynamic-pricing-rules/{id}/publish`

- 权限：`ticket.dynamic_pricing.publish`
- 必填幂等键：`idempotency_key`
- 仅允许从 `draft` 发布为 `published`。
- 已上架、未挂酒吧的票种（跨域组合通票自第十八批 AS 段第 297 条起，域内票自第十八批 BP 段第 300 条起）发布草稿规则时，规则表内仍为 `draft`、存进该票种规则草稿版，返回 `status` 为 `draft`，随票种规则版本获批后才置 `published`（见票务 PRD §5）。

请求示例：

```json
{
  "idempotency_key": "dyn-publish-001",
  "action": "publish",
  "reason": "运营审批通过",
  "evidence_attachment_ids": ["att_publish_001"]
}
```

### 4.4 模拟价格

`POST /api/v1/ticketing/dynamic-pricing/simulations`

- 权限：`ticket.dynamic_pricing.simulate`
- 必填幂等键：`idempotency_key`
- 只返回模拟结果，不创建订单，不修改票种价格，不影响已售票；模拟可覆盖草稿和已发布规则。

请求示例：

```json
{
  "idempotency_key": "dyn-sim-001",
  "ticket_type_id": "ticket_type_1",
  "visit_date": "2026-06-20",
  "sales_channel": "web",
  "context": {
    "at_time": "21:00",
    "day_sold": 60
  }
}
```

响应示例：

```json
{
  "ticket_type_id": "ticket_type_1",
  "visit_date": "2026-06-20",
  "sales_channel": "web",
  "base_price_cent": 11900,
  "adjustment_cent": 2380,
  "final_price_cent": 14280,
  "matched_rule_id": "dynamic_rule_1",
  "explanation": "命中规则：夏季加价"
}
```

## 5. 规则解释

- 基础价仍来自票种的平日/周末/节假日价。
- 多条规则命中时按 `priority DESC, updated_at DESC, id` 选择第一条。
- 模拟结果的 `explanation` 为中文：未命中时「未命中动态定价规则」，命中时「命中规则：」加规则名（18AW 收尾复核修正，PGM:287 界面全中文）。
- `condition_rule` 由服务端自算（第十八批 BP 段第 359 条起）：时段按下单时刻（项目所在地时间）所在的每日时段判，左闭右开，结束早于开始即跨零点；`day_sold_min` 按该单到访日该票种已售张数（未取消订单张数减已退的票，与票种限量的已售数同口径，不含本单）判；条件为空即不限。下单请求不收上下文；模拟可用 `context.at_time`（试算时刻，空＝此刻）与 `context.day_sold`（试算当天已售张数，空＝按实际统计）指定，`context` 其余键不读，格式不对 400 `VALIDATION_FAILED`。
- `percent_delta` 使用基础价乘以百分比，四舍五入到分。
- `fixed_delta` 使用固定金额增减。
- `fixed_price` 命中即公开价为该固定价（与派生价固定价同口径）。
- 调整后价格先应用 `min_price_cent`，再应用 `max_price_cent`。

## 6. 审计事件

| 事件 | 触发时机 |
| --- | --- |
| `ticket_dynamic_pricing.created.success` | 创建或维护规则草稿 |
| `ticket_dynamic_pricing.published.success` | 发布规则 |
| `ticket_dynamic_pricing.simulated.success` | 执行模拟 |

## 7. 明确不进入本期

- 已发布且匹配的规则会影响 `POST /api/v1/ticketing/orders` 的正式订单金额；草稿规则不影响订单。
- 不自动同步第三方渠道价。
- 不做自动调价审批。
- 不接入 AI 预测模型。

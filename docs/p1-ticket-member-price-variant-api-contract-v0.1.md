# P1 票务会员价规则接口契约 V0.1

## 1. 范围

本切片在既有票价规则接口中补齐 `member` 会员价变体。会员价基于平日/周末/节假日基础价生成，也允许后续扩展为固定价；本次不新增数据库表和新接口。

## 2. 接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/v1/ticketing/price-rules` | `ticket.price_variant.manage` | 创建票价变体规则，`variant_type` 允许 `member` |
| GET | `/api/v1/ticketing/price-rules` | `ticket.price_variant.view` | 查询票价规则 |

## 3. 示例

```json
{
  "idempotency_key": "member-price-rule-once",
  "ticket_type_id": "ticket_type_1",
  "variant_type": "member",
  "price_mode": "derive_from_calendar_price",
  "discount_rate": 90.00,
  "eligibility_rule": {
    "member_level": "standard"
  }
}
```

## 4. 验收

- `variant_type=member` 可创建成功。
- 会员价可作为订单 `price_rule_id` 使用。
- `derive_from_calendar_price` 继续按访问日期选择平日/周末/节假日基础价后乘以折扣。
- 不改变 P0 已确认的平日/周末/节假日基础价权威口径。

# P1 客人积分明细与积分兑换 API 契约 V0.1

文档类型：Implementation / API Contract

适用范围：客人 App / 小程序 P1。本文不修改 P0 OpenAPI；进入正式 P1 OpenAPI 生成前，以本文作为后端、前端和测试的人工契约。

## 1. 边界

| 项目 | 决策 |
|---|---|
| 认证 | 仅接受 `X-Customer-Token` 或 `Authorization: Bearer <customer-token>` |
| 员工权限 | 不要求员工权限，不调用 `AuthorizationService.requirePermission` |
| 积分余额权威 | `customer_points_accounts` |
| 积分明细 | `customer_points_ledger` 只追加，不由客人端直接写 |
| 兑换目录 | `customer_redemption_catalog_items`，P1 当前只查询 active 且在有效期内的条目 |
| 兑换扣库存 | 成功兑换必须调用库存服务扣减礼品库库存；库存失败时不得扣积分、不得生成兑换记录 |
| 领取核销 | 不在本切片；兑换成功状态为 `pending_pickup` |

## 2. API

| Method | Path | 用途 | 幂等 |
|---|---|---|---|
| GET | `/api/v1/customer/points/ledger` | 查询本人积分流水 | 否 |
| GET | `/api/v1/customer/points/redemption-items` | 查询可兑换礼品目录 | 否 |
| POST | `/api/v1/customer/points/redemptions` | 发起积分兑换 | 是，`Idempotency-Key` 必填 |
| GET | `/api/v1/customer/points/redemptions` | 查询本人兑换记录 | 否 |

## 3. POST Request

```json
{
  "idempotency_key": "redeem-once",
  "catalog_item_id": "gift_1",
  "quantity": 1,
  "source_terminal": "miniapp"
}
```

规则：

| 字段 | 规则 |
|---|---|
| `catalog_item_id` | 必填，必须存在且 active，且当前时间在有效期内 |
| `quantity` | 可空，默认 1；必须大于 0 |
| `source_terminal` | 可空，默认 `miniapp` |
| 积分成本 | 以后端目录项 `points_cost * quantity` 为准，前端不得传 |
| 库存扣减 | 以后端目录项 `inventory_item_id`、`stock_location_id`、`stock_quantity * quantity` 为准，前端不得传 |

## 4. Errors

| Code | HTTP | 场景 |
|---|---:|---|
| `CUSTOMER_UNAUTHENTICATED` | 401 | 缺少、无效或过期客人 Token |
| `VALIDATION_FAILED` | 400 | 缺少幂等键、目录项、数量非法 |
| `REDEMPTION_ITEM_NOT_AVAILABLE` | 404 | 兑换项不存在、非 active 或不在有效期内 |
| `POINTS_INSUFFICIENT` | 409 | 积分余额不足 |
| `STOCK_INSUFFICIENT` | 409 | 库存服务拒绝扣减 |
| `IDEMPOTENCY_CONFLICT` | 409 | 同一幂等键重复提交但请求体不同 |

## 5. Events

| Event | 触发 |
|---|---|
| `customer.points_ledger_viewed` | 查询积分流水成功 |
| `customer.points_redemption_items_viewed` | 查询可兑换目录成功 |
| `customer.points_redeemed` | 兑换成功 |
| `customer.points_redeem_failed` | 兑换失败 |
| `customer.points_redemptions_viewed` | 查询兑换记录成功 |

## 6. Anti-Patterns

| Don't | Do Instead |
|---|---|
| 让前端传积分成本 | 后端目录项计算 |
| 让前端传库存物品和库位 | 后端目录项绑定 |
| 库存失败后仍扣积分 | 库存成功后再写积分和兑换记录 |
| 把 P1 路由写入 P0 OpenAPI | 保持 P1 独立契约，P0 契约闸口不漂移 |
| 客人端复用员工权限 | 客人 Token 独立边界 |

## 7. Test Cases

| ID | 验收 |
|---|---|
| TC-01 | 客人只能查本人积分流水 |
| TC-02 | 兑换目录只返回 active 且在有效期内的条目 |
| TC-03 | 积分和库存充足时，兑换成功、扣积分、写流水、写兑换记录、扣库存 |
| TC-04 | 积分不足时，返回 `POINTS_INSUFFICIENT`，不调用库存 |
| TC-05 | 库存失败时，积分余额、流水和兑换记录均不改变 |
| TC-06 | 同一幂等键重复提交返回同一兑换结果 |

## 8. References

| Topic | Location |
|---|---|
| 客人端 P1 页面口径 | `docs/customer-app-miniapp-development-design-v0.1.md#45-会员积分与积分兑换兑换扣库存finding-pass-2-联动` |
| 终端层 P1 口径 | `docs/web-mobile-app-terminal-extension-product-prd-v0.1.md#4-客人购票-app小程序-p0` |
| 库存服务 | `backend/src/main/java/com/complex/platform/inventory/InventoryService.java` |
| 本切片测试 | `backend/src/test/java/com/complex/platform/customer/CustomerServiceTests.java`、`CustomerControllerTests.java`、`CustomerJdbcIntegrationTests.java` |

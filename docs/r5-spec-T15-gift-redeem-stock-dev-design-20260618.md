# T15 赠饮核销→0 元点单→扣吧台库存 可编码开发规格 V0.1（2026-06-18）

> **任务**：R5 backlog **T15**（W1.5，PRD §126 已 P0），跨票务×酒吧×供应链域。
> **裁决依据**：赠饮触发方式二轮裁决——**建议票务核销事件推送 → bar 生成 0 元点单扣库存**（开发期定，本规格采用事件驱动）。
> **现状基准**（已核）：`bar_production_tickets`/`bar_production_ticket_events`(V029，出品全工作流 accept/start_prepare/mark_ready/deliver/cancel/exception) 已在，但**无 `inventory_movement_id`**；ticketing 核销与 bar 扣库存间**无代码连接**；扣库存白名单已有 `redeem_consume`（`InventoryStockMovementService`）。
> **迁移**：取下一连续版本（示意 **V072**）；测试用 `MigrationVersions.latest()`。

---

## 1. 链路（事件驱动）
```
工作手机核销赠饮权益(issued_entitlements.entitlement_type='gift_drink')
  → TicketVerificationService 成功核销 → 发事件 ticket.gift_verified{entitlementId, itemRef, barId, tableSessionId}
  → BarGiftConsumer 监听 → 生成 0 元 bar_order(order_type='gift', amount=0)
  → 出品 deliver → atomicDecreaseStock(redeem_consume) → 写 inventory_stock_movements
  → bar_production_ticket_events.inventory_movement_id 回填关联
```

## 2. 数据模型（迁移草案）
```sql
-- 出品事件关联库存流水（修 R5 缺口：events 无库存关联）
ALTER TABLE bar_production_ticket_events
  ADD COLUMN IF NOT EXISTS inventory_movement_id VARCHAR(64);

-- 赠饮兑换桥（幂等：一次核销至多一单），可选独立桥表
CREATE TABLE IF NOT EXISTS bar_gift_redemptions (
    id VARCHAR(64) PRIMARY KEY,
    entitlement_id VARCHAR(64) NOT NULL,
    verification_id VARCHAR(64) NOT NULL,
    bar_order_id VARCHAR(64) NOT NULL REFERENCES bar_orders(id),
    inventory_movement_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'created',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_bar_gift_redemption_verification UNIQUE (verification_id),
    CONSTRAINT chk_bar_gift_redemption_status CHECK (status IN ('created','delivered','reversed'))
);
```
> `bar_orders.order_type` 若无 gift 枚举值需补 CHECK；0 元单不进收入但进账单/留痕。

## 3. 逻辑
- `TicketVerificationService`：赠饮权益核销成功后发 `ticket.gift_verified`（避免与 T4/T5 核销主链耦合）。
- `BarGiftConsumer`（bar 域）：消费事件 → 建 0 元 bar_order + bar_production_ticket → 出品 `deliver` 时调 `atomicDecreaseStock(movement_type=redeem_consume)` → 回填 `inventory_movement_id` 到 event 与 `bar_gift_redemptions`。**幂等**：`uq_bar_gift_redemption_verification` 防一次核销重复生成单。
- **冲正联动**（接 T5）：若该核销被 reverse，`bar_gift_redemptions.status=reversed` 并回补库存（反向 movement）。
- 赠送权益成本归**娱乐平台公司**承担（2026-07-28 口径，原写「平台」；不计商户应收）。

## 4. API / 事件 / 权限
- 主路径事件驱动，无新增对外端点（核销端点复用 T4/T5）；运营查询：`GET /api/v1/bar/gift-redemptions`。
- 事件：`ticket.gift_verified`、`bar.gift_order_created`、`bar.gift_delivered`（含 inventory_movement_id）、`bar.gift_reversed`。
- 权限：复用核销/出品权限。

## 5. 验收（GWT）
- Given 票内赠饮权益，When 工作手机核销，Then 生成 0 元 bar_order 并出品扣吧台库存，inventory_movement_id 可追溯。
- Given 同一核销重复触发，Then 幂等只生成一单（唯一键）。
- Given 核销被冲正(T5)，Then 赠饮单 reversed 且库存回补。
- 端到端集成测试覆盖 核销→0元单→扣减→对账 全链（修 R5 "无端到端验收"缺口）。

## 6. 测试 / 依赖 / claims
- 测试：链路集成、幂等、冲正回补、0 元单不计收入但进账单；`MigrationVersions.latest()`。
- 依赖：T4/T5（核销）、`InventoryStockMovementService`(redeem_consume)、`bar_production_ticket_events`。
- **claims**：bar_production_ticket_events.inventory_movement_id present（修 REQ-23 DRIFT）；矩阵 REQ-23 行下修转"已落地（端到端打通）"。

## 7. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T15 规格（事件驱动 赠饮核销→0元单→redeem_consume 扣减→inventory_movement_id 关联+幂等+冲正回补） | R5 backlog T15；二轮裁决（事件推送）；现状取自 V013/V029 |

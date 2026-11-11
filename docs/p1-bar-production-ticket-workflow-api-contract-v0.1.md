# P1 酒吧出品票据状态闭环 API 合同 V0.11

## 1. 边界

本合同覆盖 P1 酒吧 POS/SaaS 增强中的出品票据后台能力。它只负责吧台/后厨接单、制作、备好、取消、异常登记和送达同步留痕；不负责前端厨房屏、机器人配送、配方 BOM 自动扣料、会员动态价、复杂套餐和视频自动关联。

P0 点酒、账单、支付、库存扣减和送达接口保持不变。服务员送达仍走 `POST /api/v1/bar/orders/{order_id}/deliver`，该入口会把未取消、未异常的出品票据同步标记为 `delivered`。

## 2. 权限

| 权限码 | 用途 | 风险 |
| --- | --- | --- |
| `bar.production_ticket.view` | 查看出品票据和出品事件 | medium |
| `bar.production_ticket.manage` | 流转出品票据状态 | high，需审计 |

## 3. 状态机

| 当前状态 | 目标状态 | 动作类型 | 说明 |
| --- | --- | --- | --- |
| `created` | `accepted` | `accept` | 吧台/后厨接单 |
| `accepted` | `preparing` | `start_prepare` | 开始制作 |
| `preparing` | `ready` | `mark_ready` | 制作完成，等待服务员取走 |
| `created`/`accepted`/`preparing` | `cancelled` | `cancel` | 取消出品，需记录原因 |
| `created`/`accepted`/`preparing` | `exception` | `exception` | 出品异常，需记录原因 |
| 任意未取消/未异常/未送达 | `delivered` | `deliver` | 由订单送达入口自动同步 |

不允许从 `created` 直接跳到 `ready`；不允许把 `cancelled`、`exception`、`delivered` 再流转回制作状态。

## 4. 接口

### 4.1 查询出品票据

- Method: `GET`
- Path: `/api/v1/bar/production-tickets`
- Permission: `bar.production_ticket.view`
- Response: `PageResponse<BarProductionTicketRecord>`

### 4.2 查询出品事件

- Method: `GET`
- Path: `/api/v1/bar/production-tickets/{ticket_id}/events`
- Permission: `bar.production_ticket.view`
- Response: `PageResponse<BarProductionTicketEventRecord>`

事件按 `created_at DESC, id` 返回。

### 4.3 流转出品状态

- Method: `POST`
- Path: `/api/v1/bar/production-tickets/{ticket_id}/transition`
- Permission: `bar.production_ticket.manage`

Request:

```json
{
  "idempotency_key": "production-ready-001",
  "target_status": "ready",
  "station_code": "bar-counter-1",
  "operator_note": "ready for waiter",
  "reason": "optional reason",
  "evidence_attachment_ids": ["att_ready"]
}
```

Response:

```json
{
  "code": "OK",
  "data": {
    "id": "ticket_order_001",
    "order_id": "order_001",
    "bar_id": "bar_1",
    "ticket_status": "ready",
    "created_at": "2026-06-14T00:00:00Z",
    "updated_at": "2026-06-14T00:05:00Z"
  }
}
```

## 5. 验收标准

1. 创建酒吧订单后按双驱动生成出品票据：卡座/VIP/扫码/后厨自营订单自动生成 `created` 票据；吧台位（table_type=`bar_counter`）直点与独立餐饮商户（merchant_type=`catering`）订单不生成（即点即取 / 第三方出品仅回流，业务HTML 983 行口径）。
2. `created -> accepted -> preparing -> ready` 逐步流转成功，每步写入 `bar_production_ticket_events` 和审计日志。
3. 非法跳转返回 `BAR_PRODUCTION_INVALID_TRANSITION`，原票据状态不变，不写事件。
4. 送达订单成功后，对应出品票据同步为 `delivered` 并写 `deliver` 事件。
5. 取消或异常的出品票据不会被送达入口覆盖为 `delivered`。
6. 写接口缺少 `idempotency_key` 返回 `VALIDATION_FAILED`。

## 6. 回滚口径

未应用环境可移除 V029、事件类和新增接口。已应用环境不得直接删表，应以后续迁移停用 `bar.production_ticket.manage` 权限并保留事件归档。

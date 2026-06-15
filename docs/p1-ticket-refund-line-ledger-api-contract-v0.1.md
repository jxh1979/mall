# P1 票务退款权益级拆分明细接口合同 V0.1

## 1. 范围

本合同定义 P1 票务退款明细查询能力。系统在申请退款时自动生成退款明细行，用于解释退款金额如何拆到订单权益，不直接触发第三方支付退款。

## 2. 接口

`GET /api/v1/ticketing/refunds/{refund_id}/lines`

权限：`ticket.refund.view`

响应示例：

```json
{
  "code": "OK",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": "refund_line_1",
        "refund_id": "ticket_refund_1",
        "order_id": "ticket_order_1",
        "entitlement_id": "ent_1",
        "entitlement_type": "entry",
        "line_type": "entitlement",
        "refund_amount_cent": 9900,
        "remaining_times_before": 1,
        "used_times_before": 0,
        "status": "requested"
      }
    ],
    "page": 1,
    "page_size": 50,
    "total": 1
  }
}
```

## 3. 生成规则

- 退款申请目标必须是票务订单。
- 若订单已有已发权益，则按权益生成 `line_type=entitlement` 明细。
- 若订单尚未发权益，则生成 `line_type=order` 明细。
- 明细行金额总和必须等于退款申请金额。
- 审批退款后，所有明细行状态同步为 `approved`。
- 不删除已发权益历史，不抹除核销历史。

## 4. 验收标准

- 查询接口必须校验 `ticket.refund.view`。
- 退款申请后可查到明细行。
- 退款审批后明细行状态变为 `approved`。
- 金额字段使用分为单位。
- 明细行必须保留退款前已用次数和剩余次数。

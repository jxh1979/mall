-- V231 强制口径「任何下单业务必须记录下单时间+支付时间」(用户2026-07-05拍板)。
--   下单时间=各表既有 created_at(所有下单表都有,不动)。本迁移只补支付时间 paid_at。
--   语义:paid_at=支付成功/确认收款/状态转已支付的**当时时刻**,与下单时间(created_at)严格区分。
--   service_orders 已有 paid_at(不动);issued_entitlements/merchant_bills/ar_ap_items 不在本次范围
--   (权益随订单支付、账单台账属结算非下单)。
--   历史回填铁律:只有确定来源(结算留痕/已知支付时刻)才回填;否则留 null,严禁拿 created_at 冒充支付时间。

-- ① 出票订单:支付确认(pay_status→paid)时写入。
ALTER TABLE ticket_orders
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

-- ② 酒吧订单:订单本身无支付生命周期,支付走其账单结算;结算时回填链上订单 paid_at。
ALTER TABLE bar_orders
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

-- ③ 酒吧账单:结账/结算成功(payment_status→paid/partial_paid)时写入。
ALTER TABLE bar_bills
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

-- ④ 游乐团体预定:本域无独立支付步,审批确认(booking_status→approved)=预定确认落点,paid_at=approved_at。
ALTER TABLE amusement_group_bookings
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

-- ⑤ 储值流水:充值即支付,topup 流水在充值成功时追加,paid_at=交易完成时刻(created_at 本身即支付时刻,非冒充);
--    consume/refund/adjust 非充值支付,paid_at 留 null。
ALTER TABLE customer_stored_value_transactions
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

-- ⑥ 历史回填(仅确定来源):
--   bar_bills:已结算账单的支付时刻取自结算流水 bar_bill_settlements.settled_at(收款留痕,权威),按账单取最早一笔。
UPDATE bar_bills b
   SET paid_at = s.first_settled_at
  FROM (
        SELECT bill_id, MIN(settled_at) AS first_settled_at
          FROM bar_bill_settlements
         GROUP BY bill_id
       ) s
 WHERE s.bill_id = b.id
   AND b.paid_at IS NULL
   AND b.payment_status IN ('paid', 'partial_paid');

--   amusement_group_bookings:已审批(approved)预定的确认时刻取自既有 approved_at(权威留痕)。
UPDATE amusement_group_bookings
   SET paid_at = approved_at
 WHERE booking_status = 'approved'
   AND approved_at IS NOT NULL
   AND paid_at IS NULL;

--   customer_stored_value_transactions:该表有 append-only 触发器(COMPLEX_APPEND_ONLY_VIOLATION 禁 UPDATE,不可变账本),
--     故历史 topup 行 paid_at 不回填、留 null;新充值由代码 INSERT 时写 paid_at=交易完成时刻(append-only 放行 INSERT)。
--     不违反语义:topup 的 paid_at 与 created_at 同刻(充值即支付),历史缺失仅影响旧数据展示为「—」,不冒充。

--   ticket_orders / bar_orders:无独立支付时刻留痕来源(pay_status=paid 仅状态无时刻、订单支付走账单),
--     历史已支付记录 paid_at 留 null(严禁用 created_at 冒充);新支付由代码写入。

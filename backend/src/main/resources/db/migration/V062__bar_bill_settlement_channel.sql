-- R3 修复 A2/A3（P1）：酒吧账单现金/POS 收款留痕 + 支付 channel 统一枚举（REQ-21 / 对账）。
-- 背景：R2 确认 bar 账单结算缺 cashier_staff_id/pos_transaction_no 字段与 payment.cash_received/pos_received 事件；
-- channel 在 finance_payment_intents 为自由字符串、无枚举、bar↔finance 无映射。
-- 方案：新建 bar_bill_settlements 结算流水表（隔离 BarBillRecord，不 churn），settle_channel 用统一词表
--（与 finance 线下支付方式 cash/bank_transfer/pos/other + aggregate_pay 对齐 => 标识映射，可按 channel 对账归并）。

CREATE TABLE IF NOT EXISTS bar_bill_settlements (
    id VARCHAR(64) PRIMARY KEY,
    bill_id VARCHAR(64) NOT NULL REFERENCES bar_bills(id),
    settle_channel VARCHAR(32) NOT NULL,
    cashier_staff_id VARCHAR(64),
    pos_transaction_no VARCHAR(128),
    settlement_amount_cent BIGINT NOT NULL,
    merchant_id VARCHAR(64),
    settled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bbs_channel CHECK (settle_channel IN ('aggregate_pay', 'cash', 'pos', 'bank_transfer', 'other')),
    CONSTRAINT chk_bbs_amount CHECK (settlement_amount_cent >= 0)
);
CREATE INDEX IF NOT EXISTS idx_bbs_bill ON bar_bill_settlements(bill_id);
CREATE INDEX IF NOT EXISTS idx_bbs_merchant ON bar_bill_settlements(merchant_id) WHERE merchant_id IS NOT NULL;

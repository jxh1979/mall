-- 2c-3 reopen 冲正：bar_bill_settlements 加 reversed 标记。
-- 背景：反结(reopenBill)此前只翻转账单状态，不冲正原结算流水；finance 日结对账聚合 + analytics 营收聚合
-- 均 SUM(settlement_amount_cent) 无 reversed 过滤 → 反结后重结，原结算+重结算同窗口双计入对账/net。
-- 修复：reopen 时把该账单现有结算流水标 reversed，聚合侧排除 reversed，避免双计。
ALTER TABLE bar_bill_settlements
    ADD COLUMN IF NOT EXISTS reversed BOOLEAN NOT NULL DEFAULT FALSE;

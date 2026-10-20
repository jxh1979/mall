-- 文件版本：V0.01
-- V520：TK-25 低消退品留款与留款抵用（第十批）。
-- 依据：BSM L1064「退款后该台位累计消费将低于该台位适用的最低消费的，不退款——该笔金额留在本台位账上，客人可继续点其他产品。
--   因退品形成的留款在原台位账单中分别记录留款金额、已抵用金额与未抵用余额；继续点其他产品时，以该笔留款抵付的部分不重复收款、
--   不重复确认收入」；裁决 DEC-R6-04、Q28。
-- 账单调整类型闭集扩两种：min_spend_retained（退品留款，不动应付、不出金）、min_spend_retained_applied（新账单抵用留款，减应付）。
-- refunded_at 约束（V124）对非 refund/refund_reversal 类型已要求为空，新类型天然归入，不重建。

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_bar_bill_adjustment_type'
           AND table_name = 'bar_bill_adjustments'
    ) THEN
        ALTER TABLE bar_bill_adjustments DROP CONSTRAINT chk_bar_bill_adjustment_type;
    END IF;
END $$;

ALTER TABLE bar_bill_adjustments
    ADD CONSTRAINT chk_bar_bill_adjustment_type CHECK (
        adjustment_type IN (
            'service_fee', 'min_spend_top_up', 'on_account', 'waiver', 'refund',
            'waiver_reversal', 'refund_reversal',
            'min_spend_retained', 'min_spend_retained_applied'
        )
    );

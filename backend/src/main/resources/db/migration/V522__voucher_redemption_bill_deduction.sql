-- 文件版本：V0.01
-- V522：TK-28 商户折扣券结账核销关联账单与抵扣额（第十批）。
-- 依据：ENUM L81「商户折扣券——到对应商户抵扣」；ENUM L83「券抵扣部分不折现」；PGM L303「只有这次操作成功完成…才算用掉」；
--   操作员裁决（2026-10-20）「一次结账一张」：每次结账只能用一张折扣券，每笔抵扣单独留痕。
-- 核销记录增：bill_type/bill_id（抵扣的账单）、deduction_amount_cent（本次实际抵扣额，面值多出部分不找零）。
-- append-only 触发器（V301）不动；历史核销行三列为空。

ALTER TABLE voucher_redemption_records
    ADD COLUMN IF NOT EXISTS bill_type varchar(32),
    ADD COLUMN IF NOT EXISTS bill_id varchar(64),
    ADD COLUMN IF NOT EXISTS deduction_amount_cent bigint;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_vrr_deduction_amount'
           AND table_name = 'voucher_redemption_records'
    ) THEN
        ALTER TABLE voucher_redemption_records
            ADD CONSTRAINT chk_vrr_deduction_amount CHECK (deduction_amount_cent IS NULL OR deduction_amount_cent > 0);
    END IF;
END $$;

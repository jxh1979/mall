-- 延期项④：退菜退酒审批驳回的资金/库存反向冲销。
-- void（退菜退酒）在 voidOrderItem 时同步施加副作用（库存回补、账单减项、已结退款过账），
-- 流水落 pending；审批驳回（rejected）= 该 void 无效，须把已施加副作用对称回滚、恢复 void 前状态。
--
-- 账单回滚以一条反向 bar_bill_adjustments 记录承载（加回应付，留下完整审计轨迹）：
--   * 未结账单原减项为 waiver（减应付）→ 反向类型 waiver_reversal（加回应付，refunded_at 为 NULL）；
--   * 已结账单原减项为 refund（结账后退款）→ 反向类型 refund_reversal（加回应付 + 对冲退款应收，
--     携带 refunded_at 用于经营日窗口净额归集，使 refund_cent 净额归零，不重复计入退款额）。
--
-- 本迁移把 chk_bar_bill_adjustment_type 扩为 现状五种 ∪ {waiver_reversal, refund_reversal}，
-- 并放宽 chk_bar_bill_adjustment_refunded_at：允许 refund / refund_reversal 携带 refunded_at，
-- 其余类型仍不得带退款时点（防误聚合）。金额口径仍满足 amount/before/after >= 0（加回为正向，天然满足）。

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
            'waiver_reversal', 'refund_reversal'
        )
    );

-- 放宽退款时点约束：refund 与 refund_reversal 携带 refunded_at（结账后退款 / 驳回冲销均落经营日窗口）；
-- 其余类型不得带退款时点。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_bar_bill_adjustment_refunded_at'
           AND table_name = 'bar_bill_adjustments'
    ) THEN
        ALTER TABLE bar_bill_adjustments DROP CONSTRAINT chk_bar_bill_adjustment_refunded_at;
    END IF;
END $$;

ALTER TABLE bar_bill_adjustments
    ADD CONSTRAINT chk_bar_bill_adjustment_refunded_at CHECK (
        (adjustment_type IN ('refund', 'refund_reversal') AND refunded_at IS NOT NULL)
        OR (adjustment_type NOT IN ('refund', 'refund_reversal') AND refunded_at IS NULL)
    );

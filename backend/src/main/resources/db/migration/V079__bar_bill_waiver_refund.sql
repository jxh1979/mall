-- 第二阶段（待确认#2）：酒吧账单免单（waiver）/退款（refund）减项机制。
-- 现状 bar_bill_adjustments 仅 service_fee / min_spend_top_up / on_account 三种加/挂账项，无减项。
-- 本迁移把 chk_bar_bill_adjustment_type 扩为 现状三种 ∪ {waiver, refund}。
--
-- 金额口径（与 chk_bar_bill_adjustment_amounts 兼容）：waiver / refund 为减项，
--   amount_cent = 减额幅度 >= 0；after_payable = before_payable - amount >= 0（服务层保证不为负）。
--   现约束仅要求三者 >= 0，减项天然满足，无需放宽约束。
--
-- refund 退款发生在结账后，需记录"退款时点"用于经营日（business day）分桶聚合 refund_cent。
--   既有 adjustments 结构无独立时点列（created_at 是登记时刻，可与窗口口径一致，但显式语义更清晰），
--   故新增 refunded_at（仅 refund 落值，其余类型为 NULL），聚合按 refunded_at 落经营日窗口归集。

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
        adjustment_type IN ('service_fee', 'min_spend_top_up', 'on_account', 'waiver', 'refund')
    );

ALTER TABLE bar_bill_adjustments
    ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMPTZ;

-- 退款仅当 adjustment_type='refund' 时携带 refunded_at；其余类型不得带退款时点（防误聚合）。
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_bar_bill_adjustment_refunded_at'
           AND table_name = 'bar_bill_adjustments'
    ) THEN
        ALTER TABLE bar_bill_adjustments
            ADD CONSTRAINT chk_bar_bill_adjustment_refunded_at CHECK (
                (adjustment_type = 'refund' AND refunded_at IS NOT NULL)
                OR (adjustment_type <> 'refund' AND refunded_at IS NULL)
            );
    END IF;
END $$;

-- 退款聚合（T7 finance_daily_close_summaries.bar.refund_cent / dayRange 经营日窗口）按 refunded_at 归集。
CREATE INDEX IF NOT EXISTS idx_bar_bill_adjustments_refund
    ON bar_bill_adjustments (refunded_at)
    WHERE adjustment_type = 'refund';

-- 独立权限点 bar.bill.refund（结账后退款，区别于 bar.bill.adjust 的结账前调整/免单）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.bill.refund', 'Refund a settled bar bill (post-settlement, deducts daily net revenue)', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('bar.bill.refund')
ON CONFLICT (role_id, permission_code) DO NOTHING;

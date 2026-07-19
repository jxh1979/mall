-- V309 分红自定义扣除项（2026-07-19 用户拍板 #35）：在 V303 默认三项（亏损结转/欠款抵扣/发放保护线）
-- 之外，支持按需新增任意条自定义扣除项。每项：名称 + 金额来源（固定额 fixed / 比例 ratio，比例可关联
-- 净利润或营收）+ 开关。配置模型可扩展（新增来源=加枚举值+纯算式分支，非硬编码固定项数）。
--
-- 落账口径（不破坏 V299 净利/劈分/税恒等，也不破坏 V303 发放额恒等的「各扣除」语义）：
--   自定义扣除项与欠款抵扣/留存同属「税后应得的各扣除」，逐项按顺序对 (税后应得 − 欠款抵扣) 余额
--   封顶扣减（不透支），合计冻结在 dividend_statements.custom_deduction_cent，逐项落算式明细行留痕。
--   发放额恒等改写：payout = principal_net − arrears_offset − custom_deduction − reserve_withheld。
--   自定义扣除额由平台留存（隐式：少发即留），本轮不单独挂应付/应收台账（如需指定去向另立项）。

-- ── 1. 自定义扣除项配置表（挂方案；随方案增删改，配置类可 DELETE 重置，历史单靠算式明细快照留痕）──
CREATE TABLE IF NOT EXISTS dividend_deduction_items (
    id varchar(64) PRIMARY KEY,
    plan_id varchar(64) NOT NULL REFERENCES dividend_plans(id),
    name varchar(200) NOT NULL,
    source_type varchar(16) NOT NULL DEFAULT 'fixed',   -- fixed 固定额 / ratio 比例
    amount_cent bigint NOT NULL DEFAULT 0,              -- fixed 用：固定扣除额（分）
    ratio_bps int NOT NULL DEFAULT 0,                   -- ratio 用：比例（万分比）
    ratio_basis varchar(24) NOT NULL DEFAULT 'net_profit', -- ratio 基数：net_profit 净利润 / revenue 营收
    enabled boolean NOT NULL DEFAULT TRUE,
    sort_order int NOT NULL DEFAULT 0,                  -- 逐项封顶扣减的先后顺序
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_ddi_source CHECK (source_type IN ('fixed', 'ratio')),
    CONSTRAINT chk_ddi_amount CHECK (amount_cent >= 0),
    CONSTRAINT chk_ddi_ratio_bps CHECK (ratio_bps BETWEEN 0 AND 10000),
    CONSTRAINT chk_ddi_basis CHECK (ratio_basis IN ('net_profit', 'revenue'))
);

CREATE INDEX IF NOT EXISTS ix_ddi_plan ON dividend_deduction_items (plan_id, sort_order);

-- ── 2. 计算单快照：自定义扣除合计 + 发放额恒等改写 ────────────────────────────
ALTER TABLE dividend_statements
    ADD COLUMN IF NOT EXISTS custom_deduction_cent bigint NOT NULL DEFAULT 0;

-- 自定义扣除非负、且不超过 (税后应得 − 欠款抵扣) 余额（逐项封顶后合计的上限，保证发放额非负）。
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_custom_deduction_range;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_custom_deduction_range
    CHECK (custom_deduction_cent >= 0
           AND custom_deduction_cent <= principal_net_cent - arrears_offset_cent);

-- 发放额恒等改写（V303 原式基础上再减自定义扣除；存量单 custom=0 时逐分等于 V303 现状）。
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_payout_identity;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_payout_identity
    CHECK (payout_cent
        = principal_net_cent - arrears_offset_cent - custom_deduction_cent - reserve_withheld_cent);

-- ── 3. 算式明细行：自定义扣除留痕行类型 ──────────────────────────────────────
ALTER TABLE dividend_statement_lines DROP CONSTRAINT IF EXISTS chk_dsl_line_type;
ALTER TABLE dividend_statement_lines ADD CONSTRAINT chk_dsl_line_type CHECK (line_type IN
    ('revenue_bar', 'revenue_bar_refund', 'revenue_ticket', 'revenue_ticket_refund',
     'cost_inventory', 'expense_rent_allocation', 'expense_energy', 'expense_property_fee',
     'expense_labor', 'expense_amortization', 'expense_other',
     'net_profit', 'loss_carryforward', 'platform_share', 'principal_gross', 'tax_withheld',
     'principal_net', 'arrears_offset', 'custom_deduction', 'reserve_withheld', 'payout'));

-- ── 4. 生产受限运行时权限（新表/新列一律再 GRANT complex_app 兜底）──
-- 配置表可增删改（S/I/U/D，方案改扣除项走整表替换）；计算单新列由既有表级 GRANT 覆盖，幂等重授兜底。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON dividend_deduction_items TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_statements TO complex_app';
    END IF;
END $$;

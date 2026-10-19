-- 文件版本：V0.01
-- V507：FS-014 正式分红依法可分配金额前置；FS-015 分红票务收入改取已确认月度分账结算额后的算式行类型。
-- 依据：BSM L1785「分红前置：正式分红应付款形成前，由财务依据专业财税核算结果及公司有效分配决议，确认依法可分配金额，
--   本次分配不得超出该金额；法定弥补亏损、提取公积等要求不受经营扣除开关影响，已在核定金额中扣除的事项不重复扣除」；
--   BSM L1774「形成正式分红应付前，须按本节分红前置确认依法可分配金额，不能直接以经营口径净利润替代」；
--   操作员裁决 2026-10-19 第 1 条「不补：每期按经营算出的金额与依法核定金额取小发放，被压下的部分留在公司，以后不单独补分」。

-- 财务登记的依法可分配金额（商户 × 期间一行；重登记覆盖，变更留痕走审计事件，算单时快照到计算单与明细行）。
CREATE TABLE dividend_legal_confirmations (
    id                       varchar(64) PRIMARY KEY,
    merchant_id              varchar(64) NOT NULL,
    period_label             varchar(16) NOT NULL,
    legal_distributable_cent bigint      NOT NULL CHECK (legal_distributable_cent >= 0),
    tax_accounting_ref       text        NOT NULL,
    resolution_ref           text        NOT NULL,
    note                     text,
    confirmed_by             varchar(64) NOT NULL,
    confirmed_at             timestamptz NOT NULL,
    CONSTRAINT uq_dividend_legal_confirmations_period UNIQUE (merchant_id, period_label)
);

-- 计算单快照依法核定额（未核定为空；预览单恒空）。
ALTER TABLE dividend_statements
    ADD COLUMN legal_distributable_cent bigint CHECK (legal_distributable_cent >= 0);

-- 本次分配 = min(经营口径候选额, 依法核定额)；未核定时退化为原经营口径（PostgreSQL LEAST 忽略 NULL）。
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_distributable;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_distributable
    CHECK (distributable_cent = LEAST(GREATEST(net_profit_cent - loss_carryforward_applied_cent, 0),
                                      legal_distributable_cent));

-- 算式行：legal_cap 依法核定额；expense_ticket_service_fee 单一酒吧门票综合服务费（FS-013 售出月单独计收后，
--   已核销直付额不再内扣，分红按 BSM L1774 作为费用扣减）。
ALTER TABLE dividend_statement_lines DROP CONSTRAINT IF EXISTS chk_dsl_line_type;
ALTER TABLE dividend_statement_lines ADD CONSTRAINT chk_dsl_line_type CHECK (line_type IN
    ('revenue_bar', 'revenue_bar_refund', 'revenue_ticket', 'revenue_ticket_refund',
     'cost_inventory', 'expense_rent_allocation', 'expense_energy', 'expense_property_fee',
     'expense_labor', 'expense_amortization', 'expense_other', 'expense_ticket_service_fee',
     'net_profit', 'loss_carryforward', 'legal_cap', 'platform_share', 'principal_gross', 'tax_withheld',
     'principal_net', 'arrears_offset', 'custom_deduction', 'reserve_withheld', 'payout'));

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_legal_confirmations TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_statements TO complex_app';
    END IF;
END $$;

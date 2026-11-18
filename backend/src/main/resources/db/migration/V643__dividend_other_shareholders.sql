-- 文件版本：V0.01
-- V643：第十八批 AQ 段第 234 条 234-R2（其他股东按工商登记比例分配）。
-- 依据：BSM:1775「娱乐平台公司、主理人和其他股东按每家酒吧工商登记的真实持股比例分配」；BSM:1757「除涉税金额外，所有业务金额一律四舍五入、
--   保留到分……多方分配产生的非涉税尾差单独留痕，并由该笔业务实际承担结算责任的主体承担」；BSM:1776「股东为个人股东……代扣个人所得税……
--   股东为公司股东的，不代扣个人所得税，按约定全额发放」；BSM:1787「分红由该酒吧公司自己从其对公账户支付给主理人与其他股东……发放走应付款项」。
-- ① dividend_plan_shareholders：方案版本行上的其他股东名单（随版本一起审批；版本行名单只插不删，新版本复制时重新取 id）。主理人比例＝10000−平台−其他股东合计，不另存列。
-- ② dividend_statements：other_shareholders_gross_cent 其他股东应得合计；rounding_residual_cent 多方各自四舍五入后的尾差（带符号，由该酒吧公司承担）。
--    劈分恒等式（原式 V299:92）改为 平台 + 主理人 + 其他股东合计 + 尾差 ＝ 可分配额。
-- ③ 算式明细行类型闭集（现行在 V616）加 shareholder_gross、shareholder_tax_withheld、shareholder_payout、rounding_residual。
-- 研发阶段无存量（UD-07），不回填。
CREATE TABLE IF NOT EXISTS dividend_plan_shareholders (
    id varchar(64) PRIMARY KEY,
    plan_id varchar(64) NOT NULL REFERENCES dividend_plans(id),
    shareholder_name varchar(200) NOT NULL,
    shareholder_type varchar(16) NOT NULL,
    share_bps int NOT NULL,
    sort_order int NOT NULL,
    CONSTRAINT chk_dps_type CHECK (shareholder_type IN ('individual', 'company')),
    CONSTRAINT chk_dps_share_bps CHECK (share_bps BETWEEN 1 AND 10000)
);

CREATE INDEX IF NOT EXISTS ix_dps_plan ON dividend_plan_shareholders (plan_id, sort_order);

ALTER TABLE dividend_statements ADD COLUMN other_shareholders_gross_cent bigint NOT NULL DEFAULT 0;
ALTER TABLE dividend_statements ADD COLUMN rounding_residual_cent bigint NOT NULL DEFAULT 0;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_other_shareholders_nonneg CHECK (other_shareholders_gross_cent >= 0);
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_split_identity;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_split_identity
    CHECK (platform_share_cent + principal_gross_cent + other_shareholders_gross_cent + rounding_residual_cent = distributable_cent);

ALTER TABLE dividend_statement_lines DROP CONSTRAINT IF EXISTS chk_dsl_line_type;
ALTER TABLE dividend_statement_lines ADD CONSTRAINT chk_dsl_line_type CHECK (line_type IN
    ('revenue_bar', 'revenue_bar_refund', 'revenue_ticket', 'revenue_ticket_refund',
     'cost_inventory', 'expense_rent_allocation', 'expense_energy', 'expense_property_fee',
     'expense_labor', 'expense_amortization', 'expense_other', 'expense_ticket_service_fee', 'tax_period',
     'net_profit', 'loss_carryforward', 'legal_cap', 'platform_share', 'principal_gross', 'tax_withheld',
     'principal_net', 'arrears_offset', 'custom_deduction', 'reserve_withheld', 'payout',
     'shareholder_gross', 'shareholder_tax_withheld', 'shareholder_payout', 'rounding_residual'));

-- 生产受限运行时权限：新表显式 GRANT（版本行名单只插不删，照 V309 写法）；计算单新列由既有表级 GRANT 覆盖，幂等重授兜底。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT ON dividend_plan_shareholders TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_statements TO complex_app';
    END IF;
END $$;

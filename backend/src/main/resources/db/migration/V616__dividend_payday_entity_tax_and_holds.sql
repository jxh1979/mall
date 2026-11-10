-- 文件版本：V0.01
-- V616：第十八批 AC 段（batch49）分红线，按步骤就地追加：第 1 节 211-R2 分红方案发放付账日；后续 210-R2、210-R3、211-R1 各节在本文件末尾追加。

-- 1) 211-R2 发放付账日。依据：BSM:1787「发放走应付款项，落在三个付账日之一，具体哪一个由该公司在分红周期配置中指定」；
--    决策台账 DEC-R3-2026-08-04-05「落三个付账日之一，具体由该公司在分红周期配置中指定」；第十八批裁决 44「分红方案加一项「发放付账日」
--    （每月三个付账日之一，出厂 5、15、25 日）」。取值 1～3＝全平台统一每月三个付账日中的第几个；建方案必填由服务层校验。
ALTER TABLE dividend_plans ADD COLUMN payout_pay_slot SMALLINT;
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_payout_pay_slot CHECK (payout_pay_slot BETWEEN 1 AND 3);

-- 2) 210-R2 税费实际申报金额登记（第十八批裁决 42：财务按公司、税种、所属期手工登记「实际申报金额」，经财务、税务、法务审核并由顶层平台公司老板
--    最终确认后生效）。依据：BSM:1760「平台只接收专业财税软件回传的数据，或保存经授权人员手工填写并通过审批的数据」「税费金额分三个阶段分别保存、
--    互不覆盖：系统计算金额、实际申报金额、实际缴纳金额」「所有税费规则和税费调整均须经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后
--    生效，提交人不得审批自己提交的调整」。本表只存分红取数要用的实际申报金额（系统计算金额、实际缴纳金额属 BSM:1760 其余分句，本段不做）。
--    登记即提交审批（不设草稿）：pending_review → approved／rejected；驳回后可重登（部分唯一索引只管待审批与已生效）。所属期按月（YYYY-MM），
--    按季申报的整季登记一条（首月～末月）。tax_type 闭集：vat 增值税、cultural_fee 文化事业建设费、surcharge 附加税（沿用 settlement_tax_configs 取值）、
--    corporate_income 企业所得税、other 其他应由该主体承担的税费。business_entity_id 松引用 business_entities（同 V318 约定，不加硬外键，服务层核存在）。
CREATE TABLE IF NOT EXISTS entity_tax_declarations (
    id VARCHAR(64) PRIMARY KEY,
    business_entity_id VARCHAR(64) NOT NULL,
    tax_type VARCHAR(32) NOT NULL,
    period_start_month VARCHAR(7) NOT NULL,
    period_end_month VARCHAR(7) NOT NULL,
    declared_amount_cent BIGINT NOT NULL,
    reason TEXT NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(16) NOT NULL,
    review_id VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    CONSTRAINT chk_etd_tax_type CHECK (tax_type IN ('vat', 'cultural_fee', 'surcharge', 'corporate_income', 'other')),
    CONSTRAINT chk_etd_period CHECK (period_start_month ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'
        AND period_end_month ~ '^[0-9]{4}-(0[1-9]|1[0-2])$' AND period_end_month >= period_start_month),
    CONSTRAINT chk_etd_amount CHECK (declared_amount_cent >= 0),
    CONSTRAINT chk_etd_status CHECK (status IN ('pending_review', 'approved', 'rejected'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_etd_entity_tax_period_active
    ON entity_tax_declarations (business_entity_id, tax_type, period_start_month, period_end_month)
    WHERE status IN ('pending_review', 'approved');
CREATE INDEX IF NOT EXISTS idx_etd_entity_status_period
    ON entity_tax_declarations (business_entity_id, status, period_end_month);
COMMENT ON TABLE entity_tax_declarations IS '经营主体税费实际申报金额登记（BSM:1760；第十八批裁决 42：财务、税务、法务审核并由最高权限确认后生效，分红扣本期已生效申报金额）';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON entity_tax_declarations TO complex_app';
    END IF;
END $$;

-- 权限（库里没有财务税务类权限码可复用，新建；范式与授予方式同 V299 finance.dividend.*：只授 role_platform_admin，其他岗位由运营分配）。
-- manage 码授出前须审批（requires_approval=TRUE，同 V217 staff.tax_bracket.manage、V388 finance.dividend.manage）：PGM:666「以下五类权限授出前须先审批
-- 才生效——……税费规则与税费调整」。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.entity_tax.view', '查看税费申报登记', 'finance', 'medium', FALSE, 'active'),
    ('finance.entity_tax.manage', '登记税费实际申报金额（提交财务、税务、法务审核与最高确认）', 'finance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', code
  FROM (VALUES ('finance.entity_tax.view'), ('finance.entity_tax.manage')) AS seed(code)
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- 3) 210-R3 分红净利润扣该期税费。依据：BSM:1774「分红基数按「净利润」算：一个周期里的经营收入（按含税实收口径），减掉这期间的全部成本、费用与税费之后的
--    余额……以及该期间的各项税费（含增值税、所得税及其他应由该主体承担的税费）」；BSM:1801「连接税务处理，拿到这期的各项税费，算出净利润」；第十八批裁决 42
--    「分红扣本期内已生效的申报金额合计；按季申报的税整季计入季末那个月」。period_tax_cent＝方案商户经营主体已生效、所属期末月落在本期内的实际申报金额合计，
--    算式明细逐税种落 tax_period 负额行；净利润恒等式改为收入 − 销货成本 − 费用 − 税费（原式 V299:90；行类型闭集沿用 V507:34-40 加 tax_period）。
ALTER TABLE dividend_statements ADD COLUMN period_tax_cent BIGINT NOT NULL DEFAULT 0;
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_net_identity;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_net_identity
    CHECK (net_profit_cent = revenue_cent - cost_cent - expense_cent - period_tax_cent);
ALTER TABLE dividend_statement_lines DROP CONSTRAINT IF EXISTS chk_dsl_line_type;
ALTER TABLE dividend_statement_lines ADD CONSTRAINT chk_dsl_line_type CHECK (line_type IN
    ('revenue_bar', 'revenue_bar_refund', 'revenue_ticket', 'revenue_ticket_refund',
     'cost_inventory', 'expense_rent_allocation', 'expense_energy', 'expense_property_fee',
     'expense_labor', 'expense_amortization', 'expense_other', 'expense_ticket_service_fee', 'tax_period',
     'net_profit', 'loss_carryforward', 'legal_cap', 'platform_share', 'principal_gross', 'tax_withheld',
     'principal_net', 'arrears_offset', 'custom_deduction', 'reserve_withheld', 'payout'));

-- 4) 211-R1 整期暂不分红／暂缓分红登记。依据：BSM:1777「若因特殊情况需要整期暂不分红或暂缓分红的，须由主理人与娱乐平台公司协商一致后执行，并记录原因与协商
--    结果」；第十八批裁决 43「平台财务在持股分红页按商户按期登记（暂不分红或暂缓分红，原因、协商结果必填，不另走审批）；该期正式单已提交审批或已发放的不能再
--    登记，草稿随登记作废；「暂不分红」该期不出正式单、利润不补分；「暂缓分红」等协商恢复后由财务登记恢复，再照常出单；月度预览照常」。hold_type：skip 暂不分红 /
--    defer 暂缓分红；lifted_by、lifted_at、lift_result 为暂缓分红恢复时登记（lift_result＝恢复的协商结果）。同一商户同一期间只能有一条未恢复的登记。
CREATE TABLE IF NOT EXISTS dividend_period_holds (
    id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    period_label VARCHAR(16) NOT NULL,
    hold_type VARCHAR(16) NOT NULL,
    reason TEXT NOT NULL,
    negotiation_result TEXT NOT NULL,
    recorded_by VARCHAR(64) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    lifted_by VARCHAR(64),
    lifted_at TIMESTAMPTZ,
    lift_result TEXT,
    CONSTRAINT chk_dph_hold_type CHECK (hold_type IN ('skip', 'defer'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_dph_merchant_period_open
    ON dividend_period_holds (merchant_id, period_label) WHERE lifted_at IS NULL;
COMMENT ON TABLE dividend_period_holds IS '整期暂不分红／暂缓分红登记（BSM:1777；第十八批裁决 43：有未恢复登记的商户该期不出正式分红单，月度预览照常）';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_period_holds TO complex_app';
    END IF;
END $$;

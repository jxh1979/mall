-- 文件版本：V0.01
-- V660：第十八批 AX 段第 273 条 273-R1、273-R2（公司向酒吧借款登记；分红前置「先偿还全部到期借款本息」）。
-- 依据：PGM:436「必须按增资、股东借款、公司间借款等真实关系登记，借款不得当作股权或分红基数。任何公司向酒吧提供的借款，出借时须写明金额、利率、
--   还款期限与还款顺序；酒吧当期可分配利润必须先偿还全部到期借款本息，剩余部分才可用于分红，存在逾期未还借款的一律不得分红，分红方案提交时须附借款清偿
--   情况」；BSM:1785「存在逾期未还借款的，分红一律优先偿还借款——当期可分配利润必须先偿还全部到期借款本息，剩余部分才可用于分红」；ENUM:301；
--   BSM:1778「关着时仍须满足法定可分配额与借款清偿前置，再按比例分配」；决策台账 DEC-R7-21、DEC-R7-41；第十八批裁决 55、65、66。
-- ① dividend_bar_loans：借款登记（只适用于酒吧，平台财务登记；只插不改）。本金、年利率、借款协议约定的到期应付利息、出借日、到期日、还款顺序必填；
--    出借方为平台内主体时填主体，外部公司只填名称。登记同事务挂一笔酒吧应付（source_type bar_loan，金额＝本金＋利息，到期日＝还款期限），
--    还款走台账付款确认与登记付款；已还多少以该应付为准。
-- ② dividend_statements.loan_repayment_cent：出单时先偿的到期未还借款本息（取小：不超过依法可分配上限之后的可分配额）；
--    chk_ds_distributable（原式 V507:28-30）改为 可分配额 ＝ LEAST(经营口径, 依法核定额) − 借款清偿。
-- ③ 算式明细行类型闭集（现行在 V643）加 loan_repayment。
-- ④ 付款确认支付类型闭集（V629 两条 CHECK，现行即 V629 全集 40 个）加 bar_loan，共 41 个。
-- 研发阶段无存量（UD-07），不回填。
CREATE TABLE IF NOT EXISTS dividend_bar_loans (
    id varchar(64) PRIMARY KEY,
    merchant_id varchar(64) NOT NULL REFERENCES merchants(id),
    lender_entity_id varchar(64) REFERENCES business_entities(id),
    lender_name varchar(200) NOT NULL,
    principal_cent bigint NOT NULL,
    annual_rate_bps int NOT NULL,
    interest_cent bigint NOT NULL,
    lent_on date NOT NULL,
    due_date date NOT NULL,
    repay_order int NOT NULL,
    note text,
    evidence_attachment_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    ar_ap_item_id varchar(64) NOT NULL,
    created_by varchar(80),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_dbl_principal CHECK (principal_cent > 0),
    CONSTRAINT chk_dbl_rate CHECK (annual_rate_bps BETWEEN 0 AND 10000),
    CONSTRAINT chk_dbl_interest CHECK (interest_cent >= 0),
    CONSTRAINT chk_dbl_due CHECK (due_date >= lent_on),
    CONSTRAINT chk_dbl_repay_order CHECK (repay_order >= 1)
);

CREATE INDEX IF NOT EXISTS ix_dbl_merchant ON dividend_bar_loans (merchant_id, repay_order, due_date);

COMMENT ON TABLE dividend_bar_loans IS '公司向酒吧借款登记（PGM:436；分红前先偿还全部到期借款本息，逾期未还不得分红）';

ALTER TABLE dividend_statements ADD COLUMN IF NOT EXISTS loan_repayment_cent bigint NOT NULL DEFAULT 0;
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_distributable;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_distributable
    CHECK (distributable_cent = LEAST(GREATEST(net_profit_cent - loss_carryforward_applied_cent, 0), legal_distributable_cent)
                                - loan_repayment_cent);
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_loan_repayment
    CHECK (loan_repayment_cent >= 0
           AND loan_repayment_cent <= LEAST(GREATEST(net_profit_cent - loss_carryforward_applied_cent, 0), legal_distributable_cent));

ALTER TABLE dividend_statement_lines DROP CONSTRAINT IF EXISTS chk_dsl_line_type;
ALTER TABLE dividend_statement_lines ADD CONSTRAINT chk_dsl_line_type CHECK (line_type IN
    ('revenue_bar', 'revenue_bar_refund', 'revenue_ticket', 'revenue_ticket_refund',
     'cost_inventory', 'expense_rent_allocation', 'expense_energy', 'expense_property_fee',
     'expense_labor', 'expense_amortization', 'expense_other', 'expense_ticket_service_fee', 'tax_period',
     'net_profit', 'loss_carryforward', 'legal_cap', 'platform_share', 'principal_gross', 'tax_withheld',
     'principal_net', 'arrears_offset', 'custom_deduction', 'reserve_withheld', 'payout',
     'shareholder_gross', 'shareholder_tax_withheld', 'shareholder_payout', 'rounding_residual', 'loan_repayment'));

ALTER TABLE payment_confirmations DROP CONSTRAINT IF EXISTS chk_payment_confirmations_payment_type;
ALTER TABLE payment_confirmations ADD CONSTRAINT chk_payment_confirmations_payment_type CHECK (payment_type IN (
    'dividend_payout', 'dividend_reserve_released', 'staff_payroll', 'deposit_refund', 'deposit_adjustment',
    'purchase_inbound', 'purchase_consignment', 'purchase_prepayment', 'ticket_distribution', 'platform_incentive_subsidy',
    'single_bar_service_fee', 'compensation_fulfillment', 'pass_domain_share', 'pass_domain_direct_settlement',
    'pass_unverified_attribution', 'dispatch_commission_transfer', 'bar_payroll_transfer', 'inventory_transfer',
    'pass_drink_settlement', 'media_performance_settlement', 'refund', 'bar_deposit_refund', 'stored_value_consume',
    'partner_voucher', 'reconciliation_difference', 'inventory_difference', 'turnover_rent_reversal', 'manual', 'bar_loan',
    'compensation_fulfillment_recovery', 'ticket_distribution_recovery', 'saved_bottle_remedy_recovery',
    'finance_refund_ar_ap_payment', 'finance_refund_stored_value_account', 'finance_refund_ticket_payment_rejection',
    'finance_refund_channel_ticket_rejection', 'finance_refund_dealer_order_refund', 'finance_refund_bar_deposit_rejection',
    'finance_refund_bar_scan_order_rejection', 'ticket_refund', 'saved_bottle_compensation'));

ALTER TABLE payment_confirmation_flows DROP CONSTRAINT IF EXISTS chk_payment_confirmation_flows_payment_type;
ALTER TABLE payment_confirmation_flows ADD CONSTRAINT chk_payment_confirmation_flows_payment_type CHECK (payment_type IN (
    'dividend_payout', 'dividend_reserve_released', 'staff_payroll', 'deposit_refund', 'deposit_adjustment',
    'purchase_inbound', 'purchase_consignment', 'purchase_prepayment', 'ticket_distribution', 'platform_incentive_subsidy',
    'single_bar_service_fee', 'compensation_fulfillment', 'pass_domain_share', 'pass_domain_direct_settlement',
    'pass_unverified_attribution', 'dispatch_commission_transfer', 'bar_payroll_transfer', 'inventory_transfer',
    'pass_drink_settlement', 'media_performance_settlement', 'refund', 'bar_deposit_refund', 'stored_value_consume',
    'partner_voucher', 'reconciliation_difference', 'inventory_difference', 'turnover_rent_reversal', 'manual', 'bar_loan',
    'compensation_fulfillment_recovery', 'ticket_distribution_recovery', 'saved_bottle_remedy_recovery',
    'finance_refund_ar_ap_payment', 'finance_refund_stored_value_account', 'finance_refund_ticket_payment_rejection',
    'finance_refund_channel_ticket_rejection', 'finance_refund_dealer_order_refund', 'finance_refund_bar_deposit_rejection',
    'finance_refund_bar_scan_order_rejection', 'ticket_refund', 'saved_bottle_compensation'));

-- 生产受限运行时权限：新表只插不改（照 V643 写法）；计算单新列由既有表级 GRANT 覆盖，幂等重授兜底。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT ON dividend_bar_loans TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_statements TO complex_app';
    END IF;
END $$;

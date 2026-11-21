-- 文件版本：V0.01
-- V730：第十八批 BR 段第 357 条 357-R1～357-R3（游乐公司按内部结算价与实际经营游乐项目的游乐商户结算）。
-- 依据：BSM:753「游乐公司再按已批准的内部结算价或域内规则，与实际提供服务的游乐商户结算。」；BSM:1671「核销或结算时把当时价格固化到流水，历史不得手改。」；
--   BSM:1673「通票分账、单店门票结付、游乐域内结算、餐饮与零售的券结算，一律沿用与积分结算相同的账期：自然月计费、次月出账；付款落在三个付账日上。」；
--   BSM:1689「通票权益核销后才按所属域的已生效规则与实际提供方结算；未核销不产生提供方结算。」；ENUM:53「采用哪种规则由域主管公司决定，商户不能决定。」；
--   决策台账 DEC-R3-2026-08-04-07；第十八批裁决 65（按台账来源逐个定支付类型）、69（付款方是平台主体的应收也须付款确认）。
-- ① amusement_projects.catalog_item_code：游乐公司为该项目引用的内部结算价目录条目（引用 internal_price_catalog.item_code，写法同 V380 四张业务表）。
-- ② amusement_domain_settlement_lines：游乐入场核销逐笔固化的域内结算行（settled＝锁定价，reversed＝核销撤销的冲减行，金额取反），按营业月入账后回写台账行。
-- ③ 付款确认支付类型闭集（现行 V681 两条 CHECK，42 个）加 amusement_domain_settlement，共 43 个；合并时以主干现行闭集为底取并集。
-- 研发阶段无存量（UD-07）。
ALTER TABLE amusement_projects ADD COLUMN IF NOT EXISTS catalog_item_code VARCHAR(80);

COMMENT ON COLUMN amusement_projects.catalog_item_code IS
    '游乐公司为该项目引用的所属商户内部结算价目录条目（internal_price_catalog.item_code）；空＝不按内部结算价与该项目所属商户结算（BSM:753；ENUM:53）';

CREATE TABLE IF NOT EXISTS amusement_domain_settlement_lines (
    id                     VARCHAR(64) PRIMARY KEY,
    verification_id        VARCHAR(64) NOT NULL,
    kind                   VARCHAR(16) NOT NULL,
    amusement_project_id   VARCHAR(64) NOT NULL,
    project_id             VARCHAR(64) NOT NULL,
    provider_merchant_id   VARCHAR(64) NOT NULL,
    payer_entity_id        VARCHAR(64) NOT NULL,
    amount_cent            BIGINT NOT NULL,
    price_catalog_entry_id VARCHAR(80) NOT NULL,
    business_at            TIMESTAMPTZ NOT NULL,
    business_date          DATE NOT NULL,
    ar_ap_item_id          VARCHAR(64) REFERENCES ar_ap_items(id),
    posted_at              TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_adsl_kind CHECK (kind IN ('settled', 'reversed')),
    CONSTRAINT uq_adsl_verification_kind UNIQUE (verification_id, kind)
);

COMMENT ON TABLE amusement_domain_settlement_lines IS
    '游乐域内结算行：游乐入场核销按项目引用的内部结算价逐笔固化（settled），核销撤销记冲减行（reversed，金额取反）；按营业月合并入账后回写 ar_ap_item_id、posted_at（BSM:753、1671、1673）';
COMMENT ON COLUMN amusement_domain_settlement_lines.verification_id IS '票务核销记录（ticket_verifications.id）';
COMMENT ON COLUMN amusement_domain_settlement_lines.project_id IS '综合体（入场所在综合体）';
COMMENT ON COLUMN amusement_domain_settlement_lines.payer_entity_id IS '付款方：该综合体游乐票收入归属的游乐公司';
COMMENT ON COLUMN amusement_domain_settlement_lines.price_catalog_entry_id IS '入场时生效的目录行（internal_price_catalog.id，即价格版本）';
COMMENT ON COLUMN amusement_domain_settlement_lines.business_date IS 'settled 取该次入场原件锁定的营业日；reversed 取撤销时刻所在营业日';
COMMENT ON COLUMN amusement_domain_settlement_lines.ar_ap_item_id IS '所入台账行；posted_at 非空而本列为空＝组净额为 0（同月全额冲回），无须结算';

ALTER TABLE payment_confirmations DROP CONSTRAINT IF EXISTS chk_payment_confirmations_payment_type;
ALTER TABLE payment_confirmations ADD CONSTRAINT chk_payment_confirmations_payment_type CHECK (payment_type IN (
    'dividend_payout', 'dividend_reserve_released', 'staff_payroll', 'deposit_refund', 'deposit_adjustment',
    'purchase_inbound', 'purchase_consignment', 'purchase_prepayment', 'ticket_distribution', 'platform_incentive_subsidy',
    'single_bar_service_fee', 'compensation_fulfillment', 'pass_domain_share', 'pass_domain_direct_settlement',
    'pass_unverified_attribution', 'dispatch_commission_transfer', 'bar_payroll_transfer', 'inventory_transfer',
    'pass_drink_settlement', 'media_performance_settlement', 'refund', 'bar_deposit_refund', 'stored_value_consume',
    'partner_voucher', 'reconciliation_difference', 'inventory_difference', 'turnover_rent_reversal', 'manual', 'bar_loan',
    'general_card_consume', 'amusement_domain_settlement',
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
    'general_card_consume', 'amusement_domain_settlement',
    'compensation_fulfillment_recovery', 'ticket_distribution_recovery', 'saved_bottle_remedy_recovery',
    'finance_refund_ar_ap_payment', 'finance_refund_stored_value_account', 'finance_refund_ticket_payment_rejection',
    'finance_refund_channel_ticket_rejection', 'finance_refund_dealer_order_refund', 'finance_refund_bar_deposit_rejection',
    'finance_refund_bar_scan_order_rejection', 'ticket_refund', 'saved_bottle_compensation'));

-- 生产受限运行时权限：结算行入账后回写台账行与入账时刻（照 V681 写法，另加 UPDATE）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON amusement_domain_settlement_lines TO complex_app';
    END IF;
END $$;

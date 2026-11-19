-- 文件版本：V0.01
-- V681：第十八批 AZ 段第 350 条 350-R1（通用卡消费挂长沙项目公司对各业态经营主体的应付）。
-- 依据：BSM:1704「长沙项目公司就通用卡余额的实际消费向各业态的经营主体直接付款结算，按三个付账日结清。」；PGM:420 同句；
--   ENUM:86「通用卡余额等同于现金：客人用它购买什么，就适用该项业务原有的分账与结算规则——购买通票按通票规则与娱乐平台公司结算，购买游乐与游乐公司结算，
--   购买演出与传媒公司结算」；决策台账 DEC-R3-2026-08-04-02；第十八批裁决 65（按台账来源逐个定支付类型）、69（付款方是平台主体的应收也须付款确认）。
-- ① general_card_consume_lines：通用卡购票消费与其回冲流水计入月度结算的记录（txn_id＝储值流水号，已记的不再取，重跑幂等）。
--    ar_ap_item_id＝所入的台账行，空＝同月全额退回、无须结算；amount_cent＝本笔计入额（消费为正、回冲为负，已扣本笔批次通路费）。
-- ② 付款确认支付类型闭集（现行 V660 两条 CHECK，41 个）加 general_card_consume，共 42 个；合并时以主干现行闭集为底取并集。
-- 研发阶段无存量（UD-07）。
CREATE TABLE IF NOT EXISTS general_card_consume_lines (
    txn_id        VARCHAR(64) PRIMARY KEY,
    ar_ap_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    amount_cent   BIGINT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE general_card_consume_lines IS '通用卡购票消费与其回冲流水计入长沙项目公司对票款收款主体月度结算的记录（BSM:1704；ENUM:86）';

ALTER TABLE payment_confirmations DROP CONSTRAINT IF EXISTS chk_payment_confirmations_payment_type;
ALTER TABLE payment_confirmations ADD CONSTRAINT chk_payment_confirmations_payment_type CHECK (payment_type IN (
    'dividend_payout', 'dividend_reserve_released', 'staff_payroll', 'deposit_refund', 'deposit_adjustment',
    'purchase_inbound', 'purchase_consignment', 'purchase_prepayment', 'ticket_distribution', 'platform_incentive_subsidy',
    'single_bar_service_fee', 'compensation_fulfillment', 'pass_domain_share', 'pass_domain_direct_settlement',
    'pass_unverified_attribution', 'dispatch_commission_transfer', 'bar_payroll_transfer', 'inventory_transfer',
    'pass_drink_settlement', 'media_performance_settlement', 'refund', 'bar_deposit_refund', 'stored_value_consume',
    'partner_voucher', 'reconciliation_difference', 'inventory_difference', 'turnover_rent_reversal', 'manual', 'bar_loan',
    'general_card_consume',
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
    'general_card_consume',
    'compensation_fulfillment_recovery', 'ticket_distribution_recovery', 'saved_bottle_remedy_recovery',
    'finance_refund_ar_ap_payment', 'finance_refund_stored_value_account', 'finance_refund_ticket_payment_rejection',
    'finance_refund_channel_ticket_rejection', 'finance_refund_dealer_order_refund', 'finance_refund_bar_deposit_rejection',
    'finance_refund_bar_scan_order_rejection', 'ticket_refund', 'saved_bottle_compensation'));

-- 生产受限运行时权限：新表只插不改（照 V660 写法）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT ON general_card_consume_lines TO complex_app';
    END IF;
END $$;

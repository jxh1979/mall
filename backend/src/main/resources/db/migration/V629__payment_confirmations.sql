-- 文件版本：V0.01
-- V629：第十八批 AM 段第 241 条 241-R1、241-R8（付款确认按全平台统一审批模型办理，按支付类型分别设流程）。
-- 依据：BSM:1803「审批通过后的发放进入应付款项台账，走付款确认。付款确认按全平台统一的审批模型执行——发起时指定这条主线的最终审批人（即其授权的
--   最高授权人），系统按组织层级逐级自动补全从发起人到该最终审批人之间的全部直接上级节点并逐级审批；财务、法务及相关部门作为平行的相关部门线，各指定
--   该部门的最终审批人并同样逐级自动补全至该部门预先设定的最低起点层级；主线与各条平行线都通过才完成付款确认；具体流程按支付类型分别设定。」；
--   第十八批裁决 55「所有应付都先走付款确认，按支付类型分别设流程」；裁决 65「按台账来源逐个分」；裁决 66「每类必带财务、法务两线；其他部门线由发起人
--   按单另加」；裁决 67「审批线页设定」；裁决 68「同收款方、同类型、同综合体可勾选合发；工资、退款、赔付按单发起」；裁决 69「付款方是平台主体的应收，
--   登记收款前也须付款确认」。
-- ① payment_confirmations：一行一个付款对象；同一张付款确认（合并发起时多行）共用 confirmation_id，复核单 object_id 取它。不设「已用」状态：
--   一个对象至多一张待审或已通过的确认（部分唯一索引），已通过即可分笔付到结清。研发阶段无存量（UD-07）。
-- ② payment_confirmation_flows：各支付类型在财务、法务两线之外还必带的平行线（裁决 67，在「组织层级与审批线」页设定）；出厂不插行（裁决 66：
--   各类无两线之外的必带线）。
-- payment_type 闭集 40 个（裁决 65）：台账来源 28 个＋平台主体付款的追收应收 3 个（裁决 69）＋财务退款待办 7 个来源＋门票退款打款＋存酒对客赔付。

CREATE TABLE payment_confirmations (
    id VARCHAR(64) PRIMARY KEY,
    confirmation_id VARCHAR(64) NOT NULL,
    payment_type VARCHAR(64) NOT NULL,
    object_type VARCHAR(32) NOT NULL,
    object_id VARCHAR(64) NOT NULL,
    amount_cent BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    review_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    requested_by VARCHAR(128) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    decided_at TIMESTAMPTZ,
    CONSTRAINT chk_payment_confirmations_amount CHECK (amount_cent > 0),
    CONSTRAINT chk_payment_confirmations_status CHECK (status IN ('pending_review', 'approved', 'rejected')),
    CONSTRAINT chk_payment_confirmations_object_type CHECK (object_type IN ('ar_ap_item', 'finance_refund', 'ticket_refund', 'saved_bottle_remedy')),
    CONSTRAINT chk_payment_confirmations_payment_type CHECK (payment_type IN (
        'dividend_payout', 'dividend_reserve_released', 'staff_payroll', 'deposit_refund', 'deposit_adjustment',
        'purchase_inbound', 'purchase_consignment', 'purchase_prepayment', 'ticket_distribution', 'platform_incentive_subsidy',
        'single_bar_service_fee', 'compensation_fulfillment', 'pass_domain_share', 'pass_domain_direct_settlement',
        'pass_unverified_attribution', 'dispatch_commission_transfer', 'bar_payroll_transfer', 'inventory_transfer',
        'pass_drink_settlement', 'media_performance_settlement', 'refund', 'bar_deposit_refund', 'stored_value_consume',
        'partner_voucher', 'reconciliation_difference', 'inventory_difference', 'turnover_rent_reversal', 'manual',
        'compensation_fulfillment_recovery', 'ticket_distribution_recovery', 'saved_bottle_remedy_recovery',
        'finance_refund_ar_ap_payment', 'finance_refund_stored_value_account', 'finance_refund_ticket_payment_rejection',
        'finance_refund_channel_ticket_rejection', 'finance_refund_dealer_order_refund', 'finance_refund_bar_deposit_rejection',
        'finance_refund_bar_scan_order_rejection', 'ticket_refund', 'saved_bottle_compensation'))
);

CREATE INDEX idx_payment_confirmations_confirmation ON payment_confirmations (confirmation_id);
CREATE UNIQUE INDEX uq_payment_confirmations_active_object ON payment_confirmations (object_type, object_id)
    WHERE status IN ('pending_review', 'approved');

CREATE TABLE payment_confirmation_flows (
    payment_type VARCHAR(64) PRIMARY KEY,
    line_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_payment_confirmation_flows_payment_type CHECK (payment_type IN (
        'dividend_payout', 'dividend_reserve_released', 'staff_payroll', 'deposit_refund', 'deposit_adjustment',
        'purchase_inbound', 'purchase_consignment', 'purchase_prepayment', 'ticket_distribution', 'platform_incentive_subsidy',
        'single_bar_service_fee', 'compensation_fulfillment', 'pass_domain_share', 'pass_domain_direct_settlement',
        'pass_unverified_attribution', 'dispatch_commission_transfer', 'bar_payroll_transfer', 'inventory_transfer',
        'pass_drink_settlement', 'media_performance_settlement', 'refund', 'bar_deposit_refund', 'stored_value_consume',
        'partner_voucher', 'reconciliation_difference', 'inventory_difference', 'turnover_rent_reversal', 'manual',
        'compensation_fulfillment_recovery', 'ticket_distribution_recovery', 'saved_bottle_remedy_recovery',
        'finance_refund_ar_ap_payment', 'finance_refund_stored_value_account', 'finance_refund_ticket_payment_rejection',
        'finance_refund_channel_ticket_rejection', 'finance_refund_dealer_order_refund', 'finance_refund_bar_deposit_rejection',
        'finance_refund_bar_scan_order_rejection', 'ticket_refund', 'saved_bottle_compensation'))
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON payment_confirmations TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON payment_confirmation_flows TO complex_app';
    END IF;
END $$;

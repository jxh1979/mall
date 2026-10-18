-- 文件版本：V0.01
-- 故障停售期间拒收的已付款单进入既有原路退款流程；补偿有效期受已确认的范围约束。
ALTER TABLE finance_refunds DROP CONSTRAINT chk_finance_refund_source;
ALTER TABLE finance_refunds
    ADD CONSTRAINT chk_finance_refund_source CHECK (
        source_type IN ('ar_ap_payment', 'stored_value_account', 'ticket_payment_rejection', 'channel_ticket_rejection')
    );

ALTER TABLE amusement_compensation_policies DROP CONSTRAINT chk_amusement_compensation_policies_bounds;
ALTER TABLE amusement_compensation_policies
    ADD CONSTRAINT chk_amusement_compensation_policies_bounds CHECK (
        min_days >= 1 AND min_days <= validity_days AND validity_days <= max_days AND max_days <= 3650
    );

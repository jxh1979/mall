-- 文件版本：V0.01
-- 经销取消退款引用统一财务真实执行结果；超收原款只分配一次，预付抵用取消沿原现金退款。
ALTER TABLE dealer_order_refunds DROP CONSTRAINT chk_dealer_order_refunds_status;
ALTER TABLE dealer_order_refunds ADD CONSTRAINT chk_dealer_order_refunds_status
 CHECK (status IN ('pending_request','pending_approval','approved','rejected','processing','failed','refunded','voided'));
ALTER TABLE dealer_order_refunds ADD COLUMN finance_refund_id VARCHAR(64) UNIQUE REFERENCES finance_refunds(id);

CREATE TABLE dealer_order_overpayment_allocations (
 id VARCHAR(64) PRIMARY KEY,
 overpayment_id VARCHAR(64) NOT NULL REFERENCES dealer_order_overpayments(id),
 usage VARCHAR(32) NOT NULL CHECK (usage IN ('prepayment','offset_order','prepayment_offset')),
 amount_cent BIGINT NOT NULL CHECK (amount_cent > 0),
 target_order_id VARCHAR(64) REFERENCES dealer_orders(id),
 confirmation_attachment_ids JSONB NOT NULL CHECK (jsonb_typeof(confirmation_attachment_ids) = 'array' AND jsonb_array_length(confirmation_attachment_ids) > 0),
 confirmed_by VARCHAR(64) NOT NULL,
 confirmed_at TIMESTAMPTZ NOT NULL,
 note VARCHAR(1000) NOT NULL,
 refund_id VARCHAR(64) UNIQUE REFERENCES dealer_order_refunds(id),
 CHECK ((usage = 'prepayment' AND target_order_id IS NULL) OR (usage IN ('offset_order','prepayment_offset') AND target_order_id IS NOT NULL))
);
CREATE INDEX idx_dealer_overpayment_allocations_source ON dealer_order_overpayment_allocations(overpayment_id, confirmed_at);
CREATE INDEX idx_dealer_overpayment_allocations_target ON dealer_order_overpayment_allocations(target_order_id);
ALTER TABLE dealer_order_refunds ADD COLUMN allocation_id VARCHAR(64) UNIQUE REFERENCES dealer_order_overpayment_allocations(id);
ALTER TABLE dealer_order_overpayments DROP CONSTRAINT chk_dealer_order_overpayments_status;
ALTER TABLE dealer_order_overpayments ADD CONSTRAINT chk_dealer_order_overpayments_status
 CHECK (status IN ('unconfirmed','partially_allocated','prepayment','offset_applied'));

ALTER TABLE finance_refunds DROP CONSTRAINT chk_finance_refund_source;
ALTER TABLE finance_refunds ADD CONSTRAINT chk_finance_refund_source CHECK (
 source_type IN ('ar_ap_payment','stored_value_account','ticket_payment_rejection','channel_ticket_rejection','dealer_order_refund')
);

-- 文件版本：V0.01
-- FS-007：退货运费、原路退款与发票依据；不回填或覆盖历史事实。
ALTER TABLE dealer_orders ADD COLUMN first_leg_adjustment_cent BIGINT NOT NULL DEFAULT 0 CHECK (first_leg_adjustment_cent >= 0);

CREATE TABLE dealer_return_freight_adjustments (
    id VARCHAR(128) PRIMARY KEY,
    return_id VARCHAR(64) NOT NULL REFERENCES dealer_returns(id),
    order_id VARCHAR(64) NOT NULL REFERENCES dealer_orders(id),
    contract_terms_version INT NOT NULL,
    basis_type VARCHAR(32) NOT NULL CHECK (basis_type IN ('locked_contract','mutual_confirmation')),
    return_freight_bearer VARCHAR(32) NOT NULL CHECK (return_freight_bearer IN ('dealer','sales_entity')),
    first_leg_refundable BOOLEAN NOT NULL,
    amount_cent BIGINT NOT NULL CHECK (amount_cent >= 0),
    offset_cent BIGINT NOT NULL DEFAULT 0 CHECK (offset_cent >= 0),
    refund_cent BIGINT NOT NULL DEFAULT 0 CHECK (refund_cent >= 0),
    review_id VARCHAR(64) UNIQUE,
    status VARCHAR(32) NOT NULL CHECK (status IN ('pending_approval','approved','applied','rejected')),
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    confirmed_by VARCHAR(64) NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL,
    applied_at TIMESTAMPTZ,
    note TEXT,
    CHECK (first_leg_refundable OR amount_cent = 0),
    CHECK (status <> 'applied' OR offset_cent + refund_cent = amount_cent)
);
CREATE UNIQUE INDEX uq_dealer_freight_active_return ON dealer_return_freight_adjustments(return_id) WHERE status <> 'rejected';
CREATE INDEX idx_dealer_freight_order ON dealer_return_freight_adjustments(order_id);

ALTER TABLE dealer_order_refunds ADD COLUMN return_id VARCHAR(64) REFERENCES dealer_returns(id);
ALTER TABLE dealer_order_refunds ADD COLUMN return_freight_adjustment_id VARCHAR(128) REFERENCES dealer_return_freight_adjustments(id);
ALTER TABLE dealer_order_refunds ADD COLUMN return_component VARCHAR(24) CHECK (return_component IN ('goods','first_leg'));
ALTER TABLE dealer_order_refunds ADD CONSTRAINT chk_dealer_refund_return_component CHECK (
    (return_id IS NULL AND return_component IS NULL AND return_freight_adjustment_id IS NULL)
    OR (return_id IS NOT NULL AND return_component IS NOT NULL AND return_component = 'goods' AND return_freight_adjustment_id IS NULL AND delivery_resolution_id IS NULL)
    OR (return_id IS NOT NULL AND return_component IS NOT NULL AND return_component = 'first_leg' AND return_freight_adjustment_id IS NOT NULL AND delivery_resolution_id IS NULL)
);
DROP INDEX uq_dealer_refund_single_review;
CREATE UNIQUE INDEX uq_dealer_refund_single_review ON dealer_order_refunds(review_id)
    WHERE delivery_resolution_id IS NULL AND return_id IS NULL;
CREATE INDEX idx_dealer_refund_return ON dealer_order_refunds(return_id) WHERE return_id IS NOT NULL;
CREATE INDEX idx_dealer_refund_return_freight ON dealer_order_refunds(return_freight_adjustment_id) WHERE return_freight_adjustment_id IS NOT NULL;

ALTER TABLE dealer_invoices ADD COLUMN basis_fingerprint VARCHAR(128);
ALTER TABLE dealer_invoices ADD COLUMN retained_goods_cent BIGINT CHECK (retained_goods_cent >= 0);
ALTER TABLE dealer_invoices ADD COLUMN retained_freight_cent BIGINT CHECK (retained_freight_cent >= 0);
ALTER TABLE dealer_invoices ADD COLUMN requires_reversal BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE dealer_invoices ADD COLUMN issue_evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE dealer_invoices ADD COLUMN reversal_status VARCHAR(24) CHECK (reversal_status IN ('requested','processing','failed','success'));
ALTER TABLE dealer_invoices ADD COLUMN reversal_reference_no VARCHAR(128);
ALTER TABLE dealer_invoices ADD COLUMN reversal_evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
CREATE UNIQUE INDEX uq_dealer_invoice_active ON dealer_invoices(order_id) WHERE status IN ('pending','issuing','issued');

-- 普通退货按真实已合格接收批次回库；多批次来源不能用任意原价代替。
ALTER TABLE dealer_return_lines ADD COLUMN source_receipt_line_id VARCHAR(128) REFERENCES dealer_delivery_receipt_lines(id);

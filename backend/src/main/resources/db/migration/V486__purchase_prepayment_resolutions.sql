-- 文件版本：V0.01
-- 采购预付未用余额与终止后的未付义务处理；保留原付款与应付事实。
CREATE TABLE purchase_prepayment_resolutions (
    id VARCHAR(64) PRIMARY KEY,
    purchase_id VARCHAR(64) NOT NULL REFERENCES inventory_purchase_requests(id),
    resolution_type VARCHAR(32) NOT NULL CHECK (resolution_type IN ('refund_unused', 'reduce_unpaid')),
    source_payment_id VARCHAR(64) REFERENCES ar_ap_payments(id),
    amount_cent BIGINT NOT NULL CHECK (amount_cent > 0),
    refund_due_date DATE,
    contract_basis TEXT NOT NULL,
    reason TEXT NOT NULL,
    evidence_attachment_ids JSONB NOT NULL,
    review_id VARCHAR(64) REFERENCES supervisor_review_decisions(review_id),
    status VARCHAR(32) NOT NULL CHECK (status IN ('pending_approval', 'approved', 'rejected', 'posted')),
    requested_by VARCHAR(64) NOT NULL REFERENCES access_users(id),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    posted_by VARCHAR(64) REFERENCES access_users(id),
    posted_at TIMESTAMPTZ,
    post_evidence_ids JSONB,
    supplier_refund_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    original_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    reversal_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    replacement_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    CHECK ((status = 'posted') = (posted_at IS NOT NULL AND posted_by IS NOT NULL)),
    CHECK (status <> 'posted' OR (resolution_type = 'refund_unused' AND supplier_refund_item_id IS NOT NULL)
        OR (resolution_type = 'reduce_unpaid' AND original_item_id IS NOT NULL AND reversal_item_id IS NOT NULL)),
    CHECK ((resolution_type='refund_unused' AND source_payment_id IS NOT NULL AND refund_due_date IS NOT NULL)
        OR (resolution_type='reduce_unpaid' AND source_payment_id IS NULL AND refund_due_date IS NULL))
);
CREATE INDEX idx_purchase_prepayment_resolution_purchase ON purchase_prepayment_resolutions(purchase_id, status);
CREATE INDEX idx_purchase_prepayment_resolution_payment ON purchase_prepayment_resolutions(source_payment_id) WHERE source_payment_id IS NOT NULL;
CREATE UNIQUE INDEX uq_purchase_prepayment_resolution_original ON purchase_prepayment_resolutions(original_item_id) WHERE original_item_id IS NOT NULL;

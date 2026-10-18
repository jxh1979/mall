-- 文件版本：V0.01
-- 退款主单保持应退占额，尝试与结果证据只追加。
ALTER TABLE finance_refunds
    ADD COLUMN source_type VARCHAR(32),
    ADD COLUMN merchant_id VARCHAR(64),
    ADD COLUMN project_id VARCHAR(64),
    ADD COLUMN requested_by VARCHAR(64),
    ADD COLUMN original_payment_ref VARCHAR(256),
    ADD COLUMN gateway_provider VARCHAR(64),
    ADD COLUMN gateway_ref VARCHAR(256),
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN succeeded_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_finance_refund_lifecycle_status
        CHECK (status IN ('requested', 'processing', 'failed', 'success')),
    ADD CONSTRAINT chk_finance_refund_source
        CHECK (source_type IN ('ar_ap_payment', 'stored_value_account')),
    ADD CONSTRAINT chk_finance_refund_success_time
        CHECK ((status = 'success') = (succeeded_at IS NOT NULL));

CREATE INDEX idx_finance_refund_scope
    ON finance_refunds(project_id, merchant_id, created_at DESC);
CREATE UNIQUE INDEX uq_finance_refund_success_reference
    ON finance_refunds(gateway_provider, gateway_ref)
    WHERE status = 'success' AND gateway_ref IS NOT NULL;

CREATE TABLE finance_refund_attempts (
    id VARCHAR(64) PRIMARY KEY,
    refund_id VARCHAR(64) NOT NULL REFERENCES finance_refunds(id),
    operation VARCHAR(16) NOT NULL CHECK (operation IN ('submit', 'retry', 'query', 'execution', 'confirm')),
    dispatch_state VARCHAR(16) CHECK (dispatch_state IN ('ready', 'claimed')),
    status VARCHAR(32) NOT NULL CHECK (status IN ('requested', 'processing', 'failed', 'success')),
    gateway_provider VARCHAR(64),
    gateway_ref VARCHAR(256),
    message TEXT,
    payment_reference VARCHAR(256),
    payment_operator_id VARCHAR(64),
    verified_by VARCHAR(64),
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_finance_refund_attempts_refund
    ON finance_refund_attempts(refund_id, created_at, id);

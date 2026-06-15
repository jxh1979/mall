CREATE TABLE IF NOT EXISTS bar_bill_adjustments (
    id VARCHAR(64) PRIMARY KEY,
    bill_id VARCHAR(64) NOT NULL REFERENCES bar_bills(id) ON DELETE CASCADE,
    adjustment_type VARCHAR(32) NOT NULL,
    amount_cent BIGINT NOT NULL DEFAULT 0,
    before_payable_amount_cent BIGINT NOT NULL,
    after_payable_amount_cent BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    requested_by_user_id VARCHAR(64) NOT NULL,
    approval_required BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    snapshot_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_bill_adjustment_type CHECK (
        adjustment_type IN ('service_fee', 'min_spend_top_up', 'on_account')
    ),
    CONSTRAINT chk_bar_bill_adjustment_amounts CHECK (
        amount_cent >= 0
        AND before_payable_amount_cent >= 0
        AND after_payable_amount_cent >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_bar_bill_adjustments_bill
    ON bar_bill_adjustments (bill_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bar_bill_adjustments_idempotency
    ON bar_bill_adjustments (bill_id, idempotency_key);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.bill.adjust', 'Adjust bar bills for service fee, minimum spend top-up and on-account', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('bar.bill.adjust')
ON CONFLICT (role_id, permission_code) DO NOTHING;

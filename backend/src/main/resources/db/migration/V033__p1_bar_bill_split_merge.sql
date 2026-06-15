CREATE TABLE IF NOT EXISTS bar_bill_operations (
    id VARCHAR(64) PRIMARY KEY,
    operation_type VARCHAR(32) NOT NULL,
    primary_bill_id VARCHAR(64) NOT NULL REFERENCES bar_bills(id) ON DELETE CASCADE,
    secondary_bill_id VARCHAR(64) REFERENCES bar_bills(id),
    generated_bill_id VARCHAR(64) REFERENCES bar_bills(id),
    amount_cent BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    requested_by_user_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    snapshot_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_bill_operations_type CHECK (operation_type IN ('split', 'merge')),
    CONSTRAINT chk_bar_bill_operations_amount CHECK (amount_cent > 0)
);

CREATE INDEX IF NOT EXISTS idx_bar_bill_operations_primary
    ON bar_bill_operations (primary_bill_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_bar_bill_operations_secondary
    ON bar_bill_operations (secondary_bill_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bar_bill_operations_idempotency
    ON bar_bill_operations (primary_bill_id, idempotency_key);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.bill.split_merge', 'Split and merge bar bills', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('bar.bill.split_merge')
ON CONFLICT (role_id, permission_code) DO NOTHING;

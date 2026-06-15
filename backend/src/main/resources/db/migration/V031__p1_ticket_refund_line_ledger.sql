CREATE TABLE IF NOT EXISTS ticket_refund_lines (
    id VARCHAR(80) PRIMARY KEY,
    refund_id VARCHAR(64) NOT NULL REFERENCES ticket_refunds(id) ON DELETE CASCADE,
    order_id VARCHAR(64) NOT NULL REFERENCES ticket_orders(id),
    entitlement_id VARCHAR(64) REFERENCES issued_entitlements(id),
    entitlement_type VARCHAR(64),
    line_type VARCHAR(32) NOT NULL,
    refund_amount_cent BIGINT NOT NULL,
    remaining_times_before INTEGER NOT NULL DEFAULT 0,
    used_times_before INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ticket_refund_lines_type CHECK (line_type IN ('order', 'entitlement')),
    CONSTRAINT chk_ticket_refund_lines_amount CHECK (refund_amount_cent > 0),
    CONSTRAINT chk_ticket_refund_lines_times CHECK (remaining_times_before >= 0 AND used_times_before >= 0),
    CONSTRAINT chk_ticket_refund_lines_status CHECK (status IN ('requested', 'approved', 'rejected', 'cancelled')),
    CONSTRAINT chk_ticket_refund_lines_entitlement CHECK (
        (line_type = 'order' AND entitlement_id IS NULL)
        OR (line_type = 'entitlement' AND entitlement_id IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_ticket_refund_lines_refund
    ON ticket_refund_lines (refund_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_ticket_refund_lines_order
    ON ticket_refund_lines (order_id, status, updated_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('ticket.refund.view', 'View ticket refund line ledger', 'ticketing', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code = 'ticket.refund.view'
ON CONFLICT (role_id, permission_code) DO NOTHING;

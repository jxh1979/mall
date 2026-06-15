CREATE TABLE IF NOT EXISTS bar_table_sessions (
    id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    business_date DATE NOT NULL,
    table_id VARCHAR(64) NOT NULL REFERENCES bar_tables(id),
    reservation_id VARCHAR(64) REFERENCES bar_reservations(id),
    customer_id VARCHAR(64),
    guest_count INTEGER NOT NULL DEFAULT 0,
    min_consumption_snapshot_cent BIGINT NOT NULL DEFAULT 0,
    allowed_pass_free_guest_count_snapshot INTEGER NOT NULL DEFAULT 0,
    sales_staff_id VARCHAR(64),
    service_staff_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    source_session_id VARCHAR(64),
    session_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_table_sessions_guest_count CHECK (guest_count >= 0),
    CONSTRAINT chk_bar_table_sessions_min_consumption CHECK (min_consumption_snapshot_cent >= 0),
    CONSTRAINT chk_bar_table_sessions_free_guest_count CHECK (allowed_pass_free_guest_count_snapshot >= 0),
    CONSTRAINT chk_bar_table_sessions_version CHECK (session_version > 0)
);

CREATE INDEX IF NOT EXISTS idx_bar_table_sessions_scope
    ON bar_table_sessions (bar_id, merchant_id, business_date, status);

CREATE INDEX IF NOT EXISTS idx_bar_table_sessions_table
    ON bar_table_sessions (table_id, status);

CREATE TABLE IF NOT EXISTS bar_table_session_adjustments (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL REFERENCES bar_table_sessions(id),
    adjustment_type VARCHAR(32) NOT NULL,
    source_session_id VARCHAR(64),
    target_session_id VARCHAR(64),
    from_table_id VARCHAR(64) REFERENCES bar_tables(id),
    to_table_id VARCHAR(64) REFERENCES bar_tables(id),
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    business_date DATE NOT NULL,
    min_consumption_before_cent BIGINT NOT NULL DEFAULT 0,
    min_consumption_after_cent BIGINT NOT NULL DEFAULT 0,
    renewed_until TIMESTAMPTZ,
    additional_min_consumption_cent BIGINT NOT NULL DEFAULT 0,
    additional_deposit_amount_cent BIGINT NOT NULL DEFAULT 0,
    additional_service_fee_cent BIGINT NOT NULL DEFAULT 0,
    reason VARCHAR(500) NOT NULL,
    requested_by_user_id VARCHAR(64) NOT NULL,
    approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    snapshot_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_table_adjustment_type CHECK (adjustment_type IN ('transfer', 'merge', 'renew')),
    CONSTRAINT chk_bar_table_adjustment_amounts CHECK (
        min_consumption_before_cent >= 0
        AND min_consumption_after_cent >= 0
        AND additional_min_consumption_cent >= 0
        AND additional_deposit_amount_cent >= 0
        AND additional_service_fee_cent >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_bar_table_session_adjustments_session
    ON bar_table_session_adjustments (session_id, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.table_session.view', 'View bar table sessions', 'bar', 'medium', FALSE, 'active'),
    ('bar.table_session.create', 'Create bar table sessions', 'bar', 'high', TRUE, 'active'),
    ('bar.table_session.transfer', 'Transfer bar table sessions', 'bar', 'high', TRUE, 'active'),
    ('bar.table_session.merge', 'Merge bar table sessions', 'bar', 'high', TRUE, 'active'),
    ('bar.table_session.renew', 'Renew bar table sessions', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'bar.table_session.view',
        'bar.table_session.create',
        'bar.table_session.transfer',
        'bar.table_session.merge',
        'bar.table_session.renew'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

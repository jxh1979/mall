CREATE TABLE IF NOT EXISTS ticket_types (
    id VARCHAR(64) PRIMARY KEY,
    ticket_name VARCHAR(120) NOT NULL,
    base_calendar_price_mode VARCHAR(64) NOT NULL,
    weekday_price_cent BIGINT,
    weekend_price_cent BIGINT,
    holiday_price_cent BIGINT,
    stock_required BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_types_status
    ON ticket_types (status);

CREATE TABLE IF NOT EXISTS entitlement_templates (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    entitlement_type VARCHAR(64) NOT NULL,
    default_total_times INTEGER NOT NULL,
    default_value_cent BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_entitlement_templates_type
    ON entitlement_templates (entitlement_type);

CREATE INDEX IF NOT EXISTS idx_entitlement_templates_status
    ON entitlement_templates (status);

CREATE TABLE IF NOT EXISTS ticket_type_entitlements (
    id VARCHAR(64) PRIMARY KEY,
    ticket_type_id VARCHAR(64) NOT NULL REFERENCES ticket_types(id) ON DELETE CASCADE,
    entitlement_template_id VARCHAR(64) NOT NULL REFERENCES entitlement_templates(id),
    quantity INTEGER NOT NULL,
    total_times INTEGER NOT NULL,
    value_amount_cent BIGINT,
    effective_rule TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_type_entitlements_ticket_type
    ON ticket_type_entitlements (ticket_type_id);

CREATE TABLE IF NOT EXISTS issued_entitlements (
    id VARCHAR(64) PRIMARY KEY,
    ticket_type_id VARCHAR(64) NOT NULL REFERENCES ticket_types(id),
    order_id VARCHAR(64) NOT NULL,
    entitlement_template_id VARCHAR(64) NOT NULL REFERENCES entitlement_templates(id),
    entitlement_code VARCHAR(128) NOT NULL UNIQUE,
    entitlement_type VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),
    venue_id VARCHAR(64),
    point_id VARCHAR(64),
    total_times INTEGER NOT NULL,
    used_times INTEGER NOT NULL,
    value_amount_cent BIGINT,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_issued_entitlements_code
    ON issued_entitlements (entitlement_code);

CREATE INDEX IF NOT EXISTS idx_issued_entitlements_scope
    ON issued_entitlements (merchant_id, venue_id, status);

CREATE TABLE IF NOT EXISTS ticket_verifications (
    id VARCHAR(64) PRIMARY KEY,
    verification_no VARCHAR(64) NOT NULL UNIQUE,
    entitlement_id VARCHAR(64) NOT NULL REFERENCES issued_entitlements(id),
    entitlement_code VARCHAR(128) NOT NULL,
    credential_type VARCHAR(64) NOT NULL,
    credential_payload VARCHAR(255) NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    point_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    result_reason VARCHAR(255),
    source_type VARCHAR(64),
    client_task_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_verifications_entitlement
    ON ticket_verifications (entitlement_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ticket_verifications_client_task
    ON ticket_verifications (client_task_id);

CREATE TABLE IF NOT EXISTS terminal_sync_tasks (
    id VARCHAR(64) PRIMARY KEY,
    device_code VARCHAR(128) NOT NULL,
    staff_id VARCHAR(64),
    client_task_id VARCHAR(128) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    verification_id VARCHAR(64),
    verification_no VARCHAR(64),
    occurred_at VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_terminal_sync_tasks_device_client UNIQUE (device_code, client_task_id)
);

CREATE INDEX IF NOT EXISTS idx_terminal_sync_tasks_device_created
    ON terminal_sync_tasks (device_code, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('ticket.product.view', 'View ticket products', 'ticketing', 'medium', FALSE, 'active'),
    ('ticket.product.manage', 'Manage ticket products', 'ticketing', 'high', TRUE, 'active'),
    ('ticket.entitlement.view', 'View ticket entitlement templates', 'ticketing', 'medium', FALSE, 'active'),
    ('ticket.entitlement.manage', 'Manage ticket entitlement templates', 'ticketing', 'high', TRUE, 'active'),
    ('ticket.order.create', 'Issue ticket entitlements', 'ticketing', 'high', TRUE, 'active'),
    ('verification.execute', 'Execute ticket verifications', 'verification', 'high', TRUE, 'active'),
    ('verification.view', 'View ticket verifications', 'verification', 'medium', FALSE, 'active'),
    ('terminal.device.bind', 'Bind and sync terminal devices', 'terminal', 'high', TRUE, 'active')
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
        'ticket.product.view',
        'ticket.product.manage',
        'ticket.entitlement.view',
        'ticket.entitlement.manage',
        'ticket.order.create',
        'verification.execute',
        'verification.view',
        'terminal.device.bind'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

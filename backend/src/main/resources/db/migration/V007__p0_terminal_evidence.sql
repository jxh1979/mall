CREATE TABLE IF NOT EXISTS terminal_devices (
    id VARCHAR(64) PRIMARY KEY,
    device_id VARCHAR(128) NOT NULL UNIQUE,
    device_code VARCHAR(128) NOT NULL UNIQUE,
    terminal_type VARCHAR(64) NOT NULL,
    binding_scope VARCHAR(255) NOT NULL,
    bound_space_id VARCHAR(64),
    mdm_device_ref VARCHAR(2048),
    kiosk_mode VARCHAR(64),
    merchant_id VARCHAR(64),
    venue_id VARCHAR(64),
    area_id VARCHAR(64),
    location_point_id VARCHAR(64),
    staff_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    bound_at TIMESTAMPTZ,
    returned_at TIMESTAMPTZ,
    unbound_at TIMESTAMPTZ,
    return_reason TEXT,
    unbind_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_terminal_devices_status
    ON terminal_devices (status);

CREATE INDEX IF NOT EXISTS idx_terminal_devices_bound_space
    ON terminal_devices (bound_space_id);

CREATE TABLE IF NOT EXISTS terminal_device_status_events (
    id VARCHAR(64) PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL,
    device_code VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason TEXT,
    staff_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_terminal_device_status_events_device_created
    ON terminal_device_status_events (device_code, created_at DESC);

CREATE TABLE IF NOT EXISTS evidence_events (
    id VARCHAR(64) PRIMARY KEY,
    device_code VARCHAR(128) NOT NULL,
    staff_id VARCHAR(64),
    event_type VARCHAR(128) NOT NULL,
    event_category VARCHAR(128),
    result VARCHAR(64),
    summary VARCHAR(500) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_evidence_events_device_received
    ON evidence_events (device_code, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_evidence_events_type_received
    ON evidence_events (event_type, received_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('terminal.device.bind', 'Bind and return terminal devices', 'terminal', 'high', TRUE, 'active'),
    ('terminal.device.unbind', 'Unbind terminal devices', 'terminal', 'high', TRUE, 'active'),
    ('evidence.event.create', 'Create terminal evidence events', 'evidence', 'medium', FALSE, 'active'),
    ('evidence.event.view', 'View terminal evidence events', 'evidence', 'medium', FALSE, 'active')
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
        'terminal.device.bind',
        'terminal.device.unbind',
        'evidence.event.create',
        'evidence.event.view'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

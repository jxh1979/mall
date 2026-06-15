CREATE TABLE IF NOT EXISTS amusement_safety_notices (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    amusement_project_id VARCHAR(64),
    notice_type VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    content_version VARCHAR(64) NOT NULL,
    content_text TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    created_by_user_id VARCHAR(64) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_safety_notices_type CHECK (notice_type IN ('safety_notice', 'waiver', 'risk_disclosure')),
    CONSTRAINT chk_amusement_safety_notices_status CHECK (status IN ('active', 'inactive', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_amusement_safety_notices_project
    ON amusement_safety_notices (project_id, amusement_project_id, status);

CREATE TABLE IF NOT EXISTS amusement_waiver_acknowledgements (
    id VARCHAR(64) PRIMARY KEY,
    notice_id VARCHAR(64) NOT NULL REFERENCES amusement_safety_notices(id) ON DELETE RESTRICT,
    project_id VARCHAR(64) NOT NULL,
    amusement_project_id VARCHAR(64),
    session_id VARCHAR(64),
    customer_id VARCHAR(64) NOT NULL,
    visitor_name VARCHAR(160) NOT NULL,
    signer_name VARCHAR(160) NOT NULL,
    acknowledgement_method VARCHAR(64) NOT NULL,
    content_version VARCHAR(64) NOT NULL,
    content_hash VARCHAR(160),
    acknowledged_at TIMESTAMPTZ NOT NULL,
    operator_user_id VARCHAR(64) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_amusement_waiver_ack_notice
    ON amusement_waiver_acknowledgements (notice_id, acknowledged_at DESC);

CREATE INDEX IF NOT EXISTS idx_amusement_waiver_ack_customer
    ON amusement_waiver_acknowledgements (customer_id, project_id, acknowledged_at DESC);

CREATE TABLE IF NOT EXISTS amusement_incidents (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    amusement_project_id VARCHAR(64),
    session_id VARCHAR(64),
    device_id VARCHAR(64),
    customer_id VARCHAR(64),
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    reported_by_user_id VARCHAR(64) NOT NULL,
    reported_channel VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    resolution VARCHAR(2000),
    closed_at TIMESTAMPTZ,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_incidents_severity CHECK (severity IN ('low', 'medium', 'high', 'critical')),
    CONSTRAINT chk_amusement_incidents_status CHECK (status IN ('created', 'processing', 'pending_external', 'resolved', 'closed', 'cancelled'))
);

CREATE INDEX IF NOT EXISTS idx_amusement_incidents_project_status
    ON amusement_incidents (project_id, amusement_project_id, status, severity, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_amusement_incidents_customer
    ON amusement_incidents (customer_id, occurred_at DESC)
 WHERE customer_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS amusement_incident_actions (
    id VARCHAR(64) PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL REFERENCES amusement_incidents(id) ON DELETE CASCADE,
    action_type VARCHAR(32) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    operator_user_id VARCHAR(64) NOT NULL,
    operator_note VARCHAR(1000) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_incident_actions_type CHECK (action_type IN ('created', 'commented', 'status_changed', 'closed'))
);

CREATE INDEX IF NOT EXISTS idx_amusement_incident_actions_incident
    ON amusement_incident_actions (incident_id, created_at ASC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('amusement.safety_notice.view', 'View amusement safety notices', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.safety_notice.manage', 'Manage amusement safety notices', 'amusement', 'high', TRUE, 'active'),
    ('amusement.waiver.acknowledge', 'Record amusement waiver acknowledgements', 'amusement', 'high', TRUE, 'active'),
    ('amusement.incident.view', 'View amusement incidents', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.incident.create', 'Create amusement incidents', 'amusement', 'high', TRUE, 'active'),
    ('amusement.incident.process', 'Process amusement incidents', 'amusement', 'high', TRUE, 'active'),
    ('amusement.incident.close', 'Close amusement incidents', 'amusement', 'high', TRUE, 'active')
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
        'amusement.safety_notice.view',
        'amusement.safety_notice.manage',
        'amusement.waiver.acknowledge',
        'amusement.incident.view',
        'amusement.incident.create',
        'amusement.incident.process',
        'amusement.incident.close'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

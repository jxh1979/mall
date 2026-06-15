CREATE TABLE IF NOT EXISTS amusement_safety_templates (
    id VARCHAR(64) PRIMARY KEY,
    template_name VARCHAR(120) NOT NULL,
    items_json JSONB NOT NULL,
    effective_from DATE,
    status VARCHAR(32) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_safety_templates_items CHECK (jsonb_typeof(items_json) = 'array' AND jsonb_array_length(items_json) > 0)
);

CREATE INDEX IF NOT EXISTS idx_amusement_safety_templates_status
    ON amusement_safety_templates (status, effective_from DESC);

CREATE TABLE IF NOT EXISTS amusement_projects (
    id VARCHAR(64) PRIMARY KEY,
    project_name VARCHAR(120) NOT NULL,
    capacity INTEGER NOT NULL,
    open_status VARCHAR(32) NOT NULL,
    safety_template_id VARCHAR(64) REFERENCES amusement_safety_templates(id),
    eligibility_rule_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_projects_capacity CHECK (capacity >= 0),
    CONSTRAINT chk_amusement_projects_open_status CHECK (open_status IN ('open', 'paused', 'closed', 'maintenance'))
);

CREATE INDEX IF NOT EXISTS idx_amusement_projects_status
    ON amusement_projects (open_status, created_at DESC);

CREATE TABLE IF NOT EXISTS amusement_project_rules (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    rule_type VARCHAR(64) NOT NULL,
    rule_value VARCHAR(1000) NOT NULL,
    effective_from VARCHAR(64),
    effective_to VARCHAR(64),
    blocking BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_project_rules_type CHECK (rule_type IN ('age', 'height', 'weight', 'capacity', 'safety_notice', 'manual'))
);

CREATE INDEX IF NOT EXISTS idx_amusement_project_rules_project_active
    ON amusement_project_rules (project_id, blocking, status, created_at DESC);

CREATE TABLE IF NOT EXISTS amusement_sessions (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    capacity INTEGER NOT NULL,
    used_capacity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_sessions_capacity CHECK (capacity >= 0),
    CONSTRAINT chk_amusement_sessions_used_capacity CHECK (used_capacity >= 0 AND used_capacity <= capacity),
    CONSTRAINT chk_amusement_sessions_time CHECK (end_time > start_time),
    CONSTRAINT chk_amusement_sessions_status CHECK (status IN ('scheduled', 'open', 'closed', 'cancelled'))
);

CREATE INDEX IF NOT EXISTS idx_amusement_sessions_project_status
    ON amusement_sessions (project_id, status, start_time DESC);

CREATE TABLE IF NOT EXISTS amusement_safety_inspections (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    template_id VARCHAR(64) NOT NULL REFERENCES amusement_safety_templates(id),
    items_json JSONB NOT NULL,
    overall_result VARCHAR(64) NOT NULL,
    inspection_status VARCHAR(64) NOT NULL,
    rectification_status VARCHAR(64) NOT NULL,
    reopen_status VARCHAR(64) NOT NULL,
    inspected_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    rechecked_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    operator_note TEXT,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_safety_inspections_items CHECK (jsonb_typeof(items_json) = 'array' AND jsonb_array_length(items_json) > 0),
    CONSTRAINT chk_amusement_safety_inspections_overall CHECK (overall_result IN ('pass', 'fail', 'pending_rectification'))
);

CREATE INDEX IF NOT EXISTS idx_amusement_safety_inspections_project_latest
    ON amusement_safety_inspections (project_id, inspected_at DESC, created_at DESC);

CREATE TABLE IF NOT EXISTS amusement_safety_actions (
    id VARCHAR(64) PRIMARY KEY,
    inspection_id VARCHAR(64) NOT NULL REFERENCES amusement_safety_inspections(id) ON DELETE CASCADE,
    action_type VARCHAR(64) NOT NULL,
    action_result VARCHAR(64) NOT NULL,
    comment TEXT,
    attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_amusement_safety_actions_inspection
    ON amusement_safety_actions (inspection_id, created_at DESC);

CREATE TABLE IF NOT EXISTS amusement_verifications (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    session_id VARCHAR(64) NOT NULL REFERENCES amusement_sessions(id) ON DELETE CASCADE,
    credential_payload VARCHAR(255) NOT NULL,
    device_id VARCHAR(128),
    staff_id VARCHAR(128),
    verification_result VARCHAR(64) NOT NULL,
    failure_reason VARCHAR(128),
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_amusement_verifications_session
    ON amusement_verifications (session_id, created_at DESC);

CREATE TABLE IF NOT EXISTS amusement_manual_overrides (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    session_id VARCHAR(64) REFERENCES amusement_sessions(id) ON DELETE SET NULL,
    override_type VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(64) NOT NULL,
    operator_note TEXT,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_manual_overrides_type CHECK (override_type IN ('allow', 'deny', 'pause', 'resume'))
);

CREATE INDEX IF NOT EXISTS idx_amusement_manual_overrides_project
    ON amusement_manual_overrides (project_id, created_at DESC);

CREATE TABLE IF NOT EXISTS amusement_device_statuses (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    device_id VARCHAR(128) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    raw_status_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_amusement_device_statuses_device UNIQUE (device_id)
);

CREATE INDEX IF NOT EXISTS idx_amusement_device_statuses_project
    ON amusement_device_statuses (project_id, status, last_seen_at DESC);

CREATE TABLE IF NOT EXISTS amusement_queue_entries (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    session_id VARCHAR(64) NOT NULL REFERENCES amusement_sessions(id) ON DELETE CASCADE,
    customer_id VARCHAR(64),
    queue_no VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_amusement_queue_entries_session
    ON amusement_queue_entries (session_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS amusement_reservations (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    session_id VARCHAR(64) NOT NULL REFERENCES amusement_sessions(id) ON DELETE CASCADE,
    customer_id VARCHAR(64),
    reservation_status VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_amusement_reservations_session
    ON amusement_reservations (session_id, reservation_status, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('amusement.device_status.view', 'View amusement device statuses', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.verification.override', 'Override amusement verification result', 'amusement', 'high', TRUE, 'active'),
    ('amusement.session.view', 'View amusement projects and sessions', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.session.manage', 'Manage amusement projects and sessions', 'amusement', 'high', TRUE, 'active'),
    ('amusement.safety_inspection.view', 'View amusement safety inspections and eligibility', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.project.manage', 'Manage amusement project rules', 'amusement', 'high', TRUE, 'active'),
    ('amusement.queue.view', 'View amusement queue entries', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.reservation.view', 'View amusement reservations', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.safety_inspection.create', 'Create amusement safety inspections', 'amusement', 'high', TRUE, 'active'),
    ('amusement.safety_inspection.approve_reopen', 'Approve amusement reopen after failed inspection', 'amusement', 'high', TRUE, 'active'),
    ('amusement.safety_inspection.recheck', 'Recheck amusement safety inspection', 'amusement', 'high', TRUE, 'active'),
    ('amusement.safety_inspection.rectify', 'Submit amusement safety rectification', 'amusement', 'high', TRUE, 'active'),
    ('amusement.safety_inspection.submit', 'Submit amusement safety inspection', 'amusement', 'high', TRUE, 'active'),
    ('amusement.safety_template.view', 'View amusement safety templates', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.safety_template.manage', 'Manage amusement safety templates', 'amusement', 'high', TRUE, 'active'),
    ('amusement.verification.view', 'View amusement verifications', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.verification.execute', 'Execute amusement verification', 'amusement', 'high', TRUE, 'active')
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
        'amusement.device_status.view',
        'amusement.verification.override',
        'amusement.session.view',
        'amusement.session.manage',
        'amusement.safety_inspection.view',
        'amusement.project.manage',
        'amusement.queue.view',
        'amusement.reservation.view',
        'amusement.safety_inspection.create',
        'amusement.safety_inspection.approve_reopen',
        'amusement.safety_inspection.recheck',
        'amusement.safety_inspection.rectify',
        'amusement.safety_inspection.submit',
        'amusement.safety_template.view',
        'amusement.safety_template.manage',
        'amusement.verification.view',
        'amusement.verification.execute'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

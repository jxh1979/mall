ALTER TABLE business_entity_bindings
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS operation_entity_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS contract_entity_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS settlement_entity_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS effective_from DATE,
    ADD COLUMN IF NOT EXISTS effective_to DATE,
    ADD COLUMN IF NOT EXISTS change_reason TEXT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE business_entity_bindings
   SET updated_at = created_at
 WHERE updated_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_business_entity_bindings_active_merchant
    ON business_entity_bindings (merchant_id)
 WHERE status = 'active'
   AND merchant_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS contracts (
    id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    contract_subject_id VARCHAR(64) NOT NULL,
    operation_subject_id VARCHAR(64),
    settlement_subject_id VARCHAR(64) NOT NULL,
    effective_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    rent_terms JSONB NOT NULL DEFAULT '{}'::jsonb,
    shareholder_ratio_for_report NUMERIC(5,2),
    attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    lifecycle_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_contracts_merchant_status
    ON contracts (merchant_id, lifecycle_status);

CREATE INDEX IF NOT EXISTS idx_contracts_effective_dates
    ON contracts (effective_date, expiry_date);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('contract.view', 'View contracts', 'platform', 'medium', FALSE, 'active'),
    ('contract.draft.manage', 'Manage contract drafts', 'platform', 'high', TRUE, 'active'),
    ('contract.lifecycle.approve', 'Approve contract lifecycle changes', 'platform', 'high', TRUE, 'active')
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
        'contract.view',
        'contract.draft.manage',
        'contract.lifecycle.approve'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

CREATE TABLE IF NOT EXISTS terminal_capability_switches (
    id VARCHAR(64) PRIMARY KEY,
    terminal_type VARCHAR(64) NOT NULL,
    capability_code VARCHAR(128) NOT NULL,
    scope_type VARCHAR(64) NOT NULL,
    scope_id VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_terminal_capability_switch_scope UNIQUE (terminal_type, capability_code, scope_type, scope_id)
);

CREATE INDEX IF NOT EXISTS idx_terminal_capability_switches_terminal
    ON terminal_capability_switches (terminal_type, capability_code, enabled);

CREATE TABLE IF NOT EXISTS terminal_kiosk_configs (
    device_id VARCHAR(128) PRIMARY KEY,
    kiosk_mode VARCHAR(64) NOT NULL,
    ticketing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    bound_scope VARCHAR(255) NOT NULL,
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_terminal_kiosk_configs_mode
    ON terminal_kiosk_configs (kiosk_mode, ticketing_enabled);

CREATE TABLE IF NOT EXISTS terminal_kiosk_heartbeats (
    device_id VARCHAR(128) PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    app_version VARCHAR(255),
    capabilities JSONB NOT NULL DEFAULT '[]'::jsonb,
    last_error_code VARCHAR(128),
    kiosk_mode VARCHAR(64),
    last_heartbeat_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_terminal_kiosk_heartbeats_status
    ON terminal_kiosk_heartbeats (status, last_heartbeat_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('terminal.switch.view', 'View terminal capability switches', 'terminal', 'medium', FALSE, 'active'),
    ('terminal.switch.manage', 'Manage terminal capability switches', 'terminal', 'high', TRUE, 'active')
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
        'terminal.switch.view',
        'terminal.switch.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

CREATE TABLE IF NOT EXISTS evidence_attachments (
    id VARCHAR(64) PRIMARY KEY,
    file_name VARCHAR(120) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    file_size BIGINT,
    business_type VARCHAR(64),
    business_id VARCHAR(64),
    attachment_role VARCHAR(64),
    object_key VARCHAR(255) NOT NULL,
    upload_url TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    checksum VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_evidence_attachments_business
    ON evidence_attachments (business_type, business_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS evidence_reviews (
    id VARCHAR(64) PRIMARY KEY,
    business_type VARCHAR(64) NOT NULL,
    business_id VARCHAR(64) NOT NULL,
    risk_level VARCHAR(64) NOT NULL,
    decision VARCHAR(64) NOT NULL,
    comment TEXT,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_evidence_reviews_business
    ON evidence_reviews (business_type, business_id, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('evidence.attachment.create', 'Create and bind evidence attachments', 'evidence', 'medium', FALSE, 'active'),
    ('evidence.review.create', 'Create evidence reviews', 'evidence', 'high', TRUE, 'active'),
    ('evidence.timeline.view', 'View evidence timelines', 'evidence', 'medium', FALSE, 'active')
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
        'evidence.attachment.create',
        'evidence.review.create',
        'evidence.timeline.view'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

CREATE TABLE IF NOT EXISTS integration_order_syncs (
    id VARCHAR(80) PRIMARY KEY,
    provider_code VARCHAR(128) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    sync_type VARCHAR(32) NOT NULL,
    external_business_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount_cent BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    provider_payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_integration_order_syncs_external UNIQUE (
        provider_code, business_type, sync_type, external_business_id
    ),
    CONSTRAINT chk_integration_order_syncs_business CHECK (business_type IN ('catering', 'retail')),
    CONSTRAINT chk_integration_order_syncs_type CHECK (sync_type IN ('order', 'refund', 'voucher_verification')),
    CONSTRAINT chk_integration_order_syncs_status CHECK (status IN ('received', 'synced', 'failed', 'difference_found')),
    CONSTRAINT chk_integration_order_syncs_amount CHECK (amount_cent >= 0),
    CONSTRAINT chk_integration_order_syncs_currency CHECK (currency = 'CNY'),
    CONSTRAINT chk_integration_order_syncs_payload CHECK (jsonb_typeof(provider_payload_json) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_integration_order_syncs_provider_status
    ON integration_order_syncs (provider_code, status, updated_at DESC);

CREATE TABLE IF NOT EXISTS integration_retry_tasks (
    id VARCHAR(80) PRIMARY KEY,
    provider_code VARCHAR(128) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    external_business_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    operator_user_id VARCHAR(64),
    resolution_note VARCHAR(500),
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_integration_retry_tasks_business CHECK (business_type IN ('catering', 'retail')),
    CONSTRAINT chk_integration_retry_tasks_status CHECK (status IN ('open', 'ready', 'running', 'failed', 'resolved', 'ignored')),
    CONSTRAINT chk_integration_retry_tasks_counts CHECK (retry_count >= 0 AND max_retry_count >= 0),
    CONSTRAINT chk_integration_retry_tasks_payload CHECK (jsonb_typeof(payload_json) = 'object'),
    CONSTRAINT chk_integration_retry_tasks_evidence CHECK (jsonb_typeof(evidence_attachment_ids) = 'array')
);

CREATE INDEX IF NOT EXISTS idx_integration_retry_tasks_status_next
    ON integration_retry_tasks (status, next_retry_at, updated_at DESC);

CREATE TABLE IF NOT EXISTS integration_compensation_tasks (
    id VARCHAR(80) PRIMARY KEY,
    provider_code VARCHAR(128) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    external_business_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    operator_user_id VARCHAR(64),
    resolution_note VARCHAR(500),
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_integration_compensation_tasks_business CHECK (business_type IN ('catering', 'retail')),
    CONSTRAINT chk_integration_compensation_tasks_status CHECK (status IN ('open', 'ready', 'running', 'failed', 'resolved', 'ignored')),
    CONSTRAINT chk_integration_compensation_tasks_counts CHECK (retry_count >= 0 AND max_retry_count >= 0),
    CONSTRAINT chk_integration_compensation_tasks_payload CHECK (jsonb_typeof(payload_json) = 'object'),
    CONSTRAINT chk_integration_compensation_tasks_evidence CHECK (jsonb_typeof(evidence_attachment_ids) = 'array')
);

CREATE INDEX IF NOT EXISTS idx_integration_compensation_tasks_status
    ON integration_compensation_tasks (status, updated_at DESC);

CREATE TABLE IF NOT EXISTS integration_reconciliation_batches (
    id VARCHAR(80) PRIMARY KEY,
    provider_code VARCHAR(128) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    expected_count INTEGER NOT NULL DEFAULT 0,
    actual_count INTEGER NOT NULL DEFAULT 0,
    difference_count INTEGER NOT NULL DEFAULT 0,
    total_difference_cent BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_integration_recon_batches_business CHECK (business_type IN ('catering', 'retail')),
    CONSTRAINT chk_integration_recon_batches_period CHECK (period_end >= period_start),
    CONSTRAINT chk_integration_recon_batches_status CHECK (status IN ('open', 'difference_found', 'matched', 'resolved', 'ignored')),
    CONSTRAINT chk_integration_recon_batches_counts CHECK (
        expected_count >= 0 AND actual_count >= 0 AND difference_count >= 0 AND total_difference_cent >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_integration_recon_batches_provider_period
    ON integration_reconciliation_batches (provider_code, business_type, period_end DESC);

CREATE TABLE IF NOT EXISTS integration_reconciliation_differences (
    id VARCHAR(80) PRIMARY KEY,
    batch_id VARCHAR(80) NOT NULL REFERENCES integration_reconciliation_batches(id) ON DELETE CASCADE,
    provider_code VARCHAR(128) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    external_business_id VARCHAR(128) NOT NULL,
    difference_type VARCHAR(64) NOT NULL,
    internal_amount_cent BIGINT NOT NULL DEFAULT 0,
    provider_amount_cent BIGINT NOT NULL DEFAULT 0,
    difference_amount_cent BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    assigned_to_user_id VARCHAR(64),
    resolution_note VARCHAR(500),
    provider_payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_integration_recon_diffs_business CHECK (business_type IN ('catering', 'retail')),
    CONSTRAINT chk_integration_recon_diffs_status CHECK (status IN ('open', 'difference_found', 'matched', 'resolved', 'ignored')),
    CONSTRAINT chk_integration_recon_diffs_amount CHECK (
        internal_amount_cent >= 0 AND provider_amount_cent >= 0 AND difference_amount_cent >= 0
    ),
    CONSTRAINT chk_integration_recon_diffs_payload CHECK (jsonb_typeof(provider_payload_json) = 'object'),
    CONSTRAINT chk_integration_recon_diffs_evidence CHECK (jsonb_typeof(evidence_attachment_ids) = 'array')
);

CREATE INDEX IF NOT EXISTS idx_integration_recon_diffs_batch
    ON integration_reconciliation_differences (batch_id, status, updated_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('integration.order_sync.view', 'View third-party order sync records', 'integration', 'medium', FALSE, 'active'),
    ('integration.order_sync.manage', 'Create third-party order sync records', 'integration', 'high', TRUE, 'active'),
    ('integration.retry.view', 'View third-party retry tasks', 'integration', 'medium', FALSE, 'active'),
    ('integration.retry.manage', 'Create and run third-party retry tasks', 'integration', 'high', TRUE, 'active'),
    ('integration.compensation.view', 'View third-party compensation tasks', 'integration', 'medium', FALSE, 'active'),
    ('integration.compensation.manage', 'Create and resolve third-party compensation tasks', 'integration', 'high', TRUE, 'active'),
    ('integration.reconciliation.view', 'View third-party reconciliation batches and differences', 'integration', 'medium', FALSE, 'active'),
    ('integration.reconciliation.manage', 'Create third-party reconciliation batches and differences', 'integration', 'high', TRUE, 'active'),
    ('integration.reconciliation.resolve', 'Resolve third-party reconciliation differences', 'integration', 'high', TRUE, 'active')
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
        'integration.order_sync.view',
        'integration.order_sync.manage',
        'integration.retry.view',
        'integration.retry.manage',
        'integration.compensation.view',
        'integration.compensation.manage',
        'integration.reconciliation.view',
        'integration.reconciliation.manage',
        'integration.reconciliation.resolve'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

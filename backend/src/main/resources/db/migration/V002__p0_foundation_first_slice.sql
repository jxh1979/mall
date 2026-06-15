CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    event_name VARCHAR(128) NOT NULL,
    event_result VARCHAR(32) NOT NULL,
    actor_type VARCHAR(64),
    actor_id VARCHAR(128),
    auth_context VARCHAR(64),
    terminal VARCHAR(64),
    permission_code VARCHAR(128),
    business_type VARCHAR(128),
    business_id VARCHAR(128),
    request_id VARCHAR(128),
    idempotency_key VARCHAR(128),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_audit_events_name_occurred_at
    ON audit_events (event_name, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_events_business
    ON audit_events (business_type, business_id);

CREATE INDEX IF NOT EXISTS idx_audit_events_idempotency_key
    ON audit_events (idempotency_key);

CREATE TABLE IF NOT EXISTS idempotency_records (
    id UUID PRIMARY KEY,
    operation_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_code VARCHAR(64),
    response_body JSONB,
    http_status INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_operation_key UNIQUE (operation_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS production_readiness_items (
    id VARCHAR(64) PRIMARY KEY,
    category VARCHAR(64) NOT NULL,
    item_name VARCHAR(256) NOT NULL,
    default_solution TEXT NOT NULL,
    owner_department VARCHAR(128) NOT NULL,
    owner_person VARCHAR(128),
    supplier_name VARCHAR(128),
    status VARCHAR(64) NOT NULL,
    target_date DATE,
    evidence_link TEXT,
    last_test_time TIMESTAMPTZ,
    blocking_scope TEXT,
    fallback_mode TEXT,
    credential_ref VARCHAR(256),
    runbook_ref VARCHAR(256) NOT NULL,
    next_action TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128),
    CONSTRAINT chk_production_readiness_status CHECK (
        status IN (
            'not_started',
            'pending_info',
            'in_progress',
            'blocked',
            'ready_for_test',
            'passed',
            'failed',
            'waived',
            'not_applicable'
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_production_readiness_items_category
    ON production_readiness_items (category);

CREATE INDEX IF NOT EXISTS idx_production_readiness_items_status
    ON production_readiness_items (status);

CREATE TABLE IF NOT EXISTS production_readiness_checks (
    id UUID PRIMARY KEY,
    check_scope VARCHAR(64) NOT NULL,
    result VARCHAR(32) NOT NULL,
    blocker_count INTEGER NOT NULL,
    operator_note TEXT,
    checked_at TIMESTAMPTZ NOT NULL,
    checked_by VARCHAR(128),
    idempotency_key VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_production_readiness_checks_checked_at
    ON production_readiness_checks (checked_at DESC);

CREATE TABLE IF NOT EXISTS production_readiness_check_blockers (
    check_id UUID NOT NULL REFERENCES production_readiness_checks(id),
    item_id VARCHAR(64) NOT NULL REFERENCES production_readiness_items(id),
    status VARCHAR(64) NOT NULL,
    blocking_scope TEXT,
    PRIMARY KEY (check_id, item_id)
);

CREATE TABLE IF NOT EXISTS permission_templates (
    id VARCHAR(64) PRIMARY KEY,
    template_name VARCHAR(128) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS permission_template_permissions (
    template_id VARCHAR(64) NOT NULL REFERENCES permission_templates(id),
    permission_code VARCHAR(128) NOT NULL,
    PRIMARY KEY (template_id, permission_code)
);

CREATE TABLE IF NOT EXISTS temporary_permission_grants (
    id UUID PRIMARY KEY,
    subject_type VARCHAR(64) NOT NULL,
    subject_id VARCHAR(128) NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    reason TEXT NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    granted_by VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_temporary_permission_grants_subject
    ON temporary_permission_grants (subject_type, subject_id);

CREATE TABLE IF NOT EXISTS business_entity_bindings (
    id UUID PRIMARY KEY,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id VARCHAR(128) NOT NULL,
    target_entity_type VARCHAR(64) NOT NULL,
    target_entity_id VARCHAR(128) NOT NULL,
    binding_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_business_entity_bindings_source
    ON business_entity_bindings (source_entity_type, source_entity_id);

CREATE INDEX IF NOT EXISTS idx_business_entity_bindings_target
    ON business_entity_bindings (target_entity_type, target_entity_id);

CREATE TABLE IF NOT EXISTS finance_ar_ap_items (
    id UUID PRIMARY KEY,
    business_entity_id VARCHAR(128) NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    due_date DATE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_finance_ar_ap_items_entity
    ON finance_ar_ap_items (business_entity_id);

CREATE TABLE IF NOT EXISTS finance_ar_ap_payments (
    id UUID PRIMARY KEY,
    ar_ap_item_id UUID NOT NULL REFERENCES finance_ar_ap_items(id),
    payment_status VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

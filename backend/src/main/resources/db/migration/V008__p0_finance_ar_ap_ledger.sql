CREATE TABLE IF NOT EXISTS ar_ap_items (
    id VARCHAR(64) PRIMARY KEY,
    item_no VARCHAR(64) NOT NULL UNIQUE,
    direction VARCHAR(32) NOT NULL,
    counterparty_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(64),
    source_id VARCHAR(64),
    amount_cent BIGINT NOT NULL,
    paid_amount_cent BIGINT NOT NULL DEFAULT 0,
    unpaid_amount_cent BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    due_date DATE NOT NULL,
    accounting_period VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ar_ap_items_direction CHECK (direction IN ('receivable', 'payable')),
    CONSTRAINT chk_ar_ap_items_amount_positive CHECK (amount_cent > 0),
    CONSTRAINT chk_ar_ap_items_paid_non_negative CHECK (paid_amount_cent >= 0),
    CONSTRAINT chk_ar_ap_items_unpaid_non_negative CHECK (unpaid_amount_cent >= 0)
);

CREATE INDEX IF NOT EXISTS idx_ar_ap_items_direction_status
    ON ar_ap_items (direction, status);

CREATE INDEX IF NOT EXISTS idx_ar_ap_items_counterparty
    ON ar_ap_items (counterparty_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ar_ap_items_direction_source
    ON ar_ap_items (direction, source_type, source_id)
 WHERE source_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS ar_ap_item_sources (
    id VARCHAR(64) PRIMARY KEY,
    ar_ap_item_id VARCHAR(64) NOT NULL REFERENCES ar_ap_items(id) ON DELETE CASCADE,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    amount_cent BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ar_ap_item_sources_amount_positive CHECK (amount_cent > 0)
);

CREATE INDEX IF NOT EXISTS idx_ar_ap_item_sources_item
    ON ar_ap_item_sources (ar_ap_item_id);

CREATE TABLE IF NOT EXISTS ar_ap_payments (
    id VARCHAR(64) PRIMARY KEY,
    ar_ap_item_id VARCHAR(64) NOT NULL REFERENCES ar_ap_items(id) ON DELETE CASCADE,
    amount_cent BIGINT NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    payment_time TIMESTAMPTZ NOT NULL,
    bank_flow_ref VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ar_ap_payments_amount_positive CHECK (amount_cent > 0)
);

CREATE INDEX IF NOT EXISTS idx_ar_ap_payments_item_created
    ON ar_ap_payments (ar_ap_item_id, created_at DESC);

CREATE TABLE IF NOT EXISTS finance_payment_intents (
    id VARCHAR(64) PRIMARY KEY,
    business_order_type VARCHAR(64) NOT NULL,
    business_order_id VARCHAR(64) NOT NULL,
    amount_cent BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    channel VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_finance_payment_intents_amount_positive CHECK (amount_cent > 0)
);

CREATE INDEX IF NOT EXISTS idx_finance_payment_intents_business_order
    ON finance_payment_intents (business_order_type, business_order_id);

CREATE TABLE IF NOT EXISTS finance_offline_payment_confirmations (
    id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    amount_cent BIGINT NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_finance_offline_confirm_amount_positive CHECK (amount_cent > 0)
);

CREATE INDEX IF NOT EXISTS idx_finance_offline_confirm_payment
    ON finance_offline_payment_confirmations (payment_id);

CREATE TABLE IF NOT EXISTS finance_refunds (
    id VARCHAR(64) PRIMARY KEY,
    target_id VARCHAR(64) NOT NULL,
    refund_amount_cent BIGINT NOT NULL,
    reason TEXT NOT NULL,
    refund_channel VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_finance_refunds_amount_positive CHECK (refund_amount_cent > 0)
);

CREATE INDEX IF NOT EXISTS idx_finance_refunds_target
    ON finance_refunds (target_id);

CREATE TABLE IF NOT EXISTS finance_daily_closes (
    id VARCHAR(64) PRIMARY KEY,
    business_date DATE NOT NULL,
    scope_type VARCHAR(64) NOT NULL,
    scope_id VARCHAR(64) NOT NULL,
    force_close BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_finance_daily_closes_scope_date
    ON finance_daily_closes (scope_type, scope_id, business_date);

CREATE TABLE IF NOT EXISTS finance_reconciliation_batches (
    id VARCHAR(64) PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL UNIQUE,
    scope_type VARCHAR(64) NOT NULL,
    scope_id VARCHAR(64) NOT NULL,
    business_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_finance_reconciliation_batches_scope_date
    ON finance_reconciliation_batches (scope_type, scope_id, business_date);

CREATE TABLE IF NOT EXISTS finance_reconciliation_differences (
    id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL REFERENCES finance_reconciliation_batches(id) ON DELETE CASCADE,
    difference_type VARCHAR(64) NOT NULL,
    business_ref VARCHAR(128) NOT NULL,
    amount_cent BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_finance_reconciliation_differences_batch
    ON finance_reconciliation_differences (batch_id);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.ar_ap.view', 'View AR/AP ledger items', 'finance', 'medium', FALSE, 'active'),
    ('finance.ar_ap.manage', 'Manage AR/AP ledger items', 'finance', 'high', TRUE, 'active'),
    ('finance.payment.register', 'Register finance payments', 'finance', 'high', TRUE, 'active'),
    ('finance.daily_close.lock', 'Lock finance daily close', 'finance', 'high', TRUE, 'active'),
    ('finance.payment_intent.create', 'Create finance payment intents', 'finance', 'high', TRUE, 'active'),
    ('finance.reconciliation.view', 'View finance reconciliation batches', 'finance', 'medium', FALSE, 'active'),
    ('openplatform.reconcile.execute', 'Execute open platform reconciliation', 'open-platform', 'high', TRUE, 'active'),
    ('finance.refund.create', 'Create finance refunds', 'finance', 'high', TRUE, 'active')
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
        'finance.ar_ap.view',
        'finance.ar_ap.manage',
        'finance.payment.register',
        'finance.daily_close.lock',
        'finance.payment_intent.create',
        'finance.reconciliation.view',
        'openplatform.reconcile.execute',
        'finance.refund.create'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

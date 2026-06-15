CREATE TABLE IF NOT EXISTS contract_key_dates (
    id varchar(80) PRIMARY KEY,
    contract_id varchar(80) NOT NULL,
    merchant_id varchar(80) NOT NULL,
    date_type varchar(40) NOT NULL,
    title varchar(160) NOT NULL,
    due_date date NOT NULL,
    reminder_days jsonb NOT NULL DEFAULT '[]'::jsonb,
    status varchar(32) NOT NULL,
    owner_user_id varchar(80) NOT NULL,
    source_type varchar(32) NOT NULL,
    human_confirmed boolean NOT NULL,
    evidence_attachment_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT chk_contract_key_dates_type CHECK (date_type IN ('contract_start', 'contract_end', 'renewal_notice', 'deposit_return', 'rent_free_end', 'custom')),
    CONSTRAINT chk_contract_key_dates_status CHECK (status IN ('active', 'completed', 'waived', 'archived')),
    CONSTRAINT chk_contract_key_dates_source CHECK (source_type IN ('manual', 'ai_suggestion', 'imported'))
);

CREATE INDEX IF NOT EXISTS idx_contract_key_dates_merchant_due
    ON contract_key_dates (merchant_id, due_date, status);

CREATE INDEX IF NOT EXISTS idx_contract_key_dates_contract_type
    ON contract_key_dates (contract_id, date_type, status);

CREATE TABLE IF NOT EXISTS compliance_expiry_items (
    id varchar(80) PRIMARY KEY,
    subject_type varchar(40) NOT NULL,
    subject_id varchar(80) NOT NULL,
    merchant_id varchar(80) NOT NULL,
    item_type varchar(40) NOT NULL,
    item_name varchar(160) NOT NULL,
    document_no varchar(120),
    issuing_authority varchar(160),
    valid_from date,
    valid_to date NOT NULL,
    reminder_days jsonb NOT NULL DEFAULT '[]'::jsonb,
    expiry_action varchar(40) NOT NULL,
    status varchar(32) NOT NULL,
    owner_user_id varchar(80) NOT NULL,
    source_type varchar(32) NOT NULL,
    human_confirmed boolean NOT NULL,
    evidence_attachment_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT chk_compliance_expiry_subject CHECK (subject_type IN ('merchant', 'contract', 'supplier', 'asset', 'amusement_device')),
    CONSTRAINT chk_compliance_expiry_item_type CHECK (item_type IN ('business_license', 'food_license', 'alcohol_license', 'fire_certificate', 'special_equipment', 'insurance', 'supplier_qualification', 'inspection_certificate', 'custom')),
    CONSTRAINT chk_compliance_expiry_action CHECK (expiry_action IN ('remind_only', 'block_operation', 'supervisor_approval')),
    CONSTRAINT chk_compliance_expiry_status CHECK (status IN ('active', 'completed', 'waived', 'archived')),
    CONSTRAINT chk_compliance_expiry_source CHECK (source_type IN ('manual', 'ai_suggestion', 'imported')),
    CONSTRAINT chk_compliance_expiry_valid_range CHECK (valid_from IS NULL OR valid_to >= valid_from)
);

CREATE INDEX IF NOT EXISTS idx_compliance_expiry_items_merchant_valid
    ON compliance_expiry_items (merchant_id, valid_to, status);

CREATE INDEX IF NOT EXISTS idx_compliance_expiry_items_subject
    ON compliance_expiry_items (subject_type, subject_id, item_type, status);

CREATE TABLE IF NOT EXISTS compliance_reminders (
    id varchar(80) PRIMARY KEY,
    source_type varchar(40) NOT NULL,
    source_id varchar(80) NOT NULL,
    reminder_type varchar(32) NOT NULL,
    due_date date NOT NULL,
    reminder_at date NOT NULL,
    severity varchar(16) NOT NULL,
    status varchar(32) NOT NULL,
    blocking_suggested boolean NOT NULL,
    message varchar(400) NOT NULL,
    generated_at timestamptz NOT NULL,
    CONSTRAINT chk_compliance_reminder_source CHECK (source_type IN ('contract_key_date', 'compliance_expiry_item')),
    CONSTRAINT chk_compliance_reminder_type CHECK (reminder_type IN ('upcoming', 'expired')),
    CONSTRAINT chk_compliance_reminder_severity CHECK (severity IN ('info', 'medium', 'high', 'critical')),
    CONSTRAINT chk_compliance_reminder_status CHECK (status IN ('open', 'acknowledged', 'closed'))
);

CREATE INDEX IF NOT EXISTS idx_compliance_reminders_status_due
    ON compliance_reminders (status, due_date, severity);

CREATE INDEX IF NOT EXISTS idx_compliance_reminders_source
    ON compliance_reminders (source_type, source_id, reminder_type);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('compliance.contract_key_date.view', 'View contract key dates', 'compliance', 'medium', FALSE, 'active'),
    ('compliance.contract_key_date.manage', 'Manage contract key dates', 'compliance', 'high', TRUE, 'active'),
    ('compliance.expiry_item.view', 'View compliance expiry items', 'compliance', 'medium', FALSE, 'active'),
    ('compliance.expiry_item.manage', 'Manage compliance expiry items', 'compliance', 'high', TRUE, 'active'),
    ('compliance.reminder.view', 'View compliance reminders', 'compliance', 'medium', FALSE, 'active'),
    ('compliance.reminder.scan', 'Run compliance reminder scan', 'compliance', 'high', TRUE, 'active')
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
        'compliance.contract_key_date.view',
        'compliance.contract_key_date.manage',
        'compliance.expiry_item.view',
        'compliance.expiry_item.manage',
        'compliance.reminder.view',
        'compliance.reminder.scan'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

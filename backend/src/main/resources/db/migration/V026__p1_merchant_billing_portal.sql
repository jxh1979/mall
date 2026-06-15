CREATE TABLE IF NOT EXISTS merchant_bills (
    id varchar(80) PRIMARY KEY,
    merchant_id varchar(80) NOT NULL,
    ar_ap_item_id varchar(80),
    bill_no varchar(80) NOT NULL UNIQUE,
    bill_period_start date NOT NULL,
    bill_period_end date NOT NULL,
    due_date date NOT NULL,
    total_amount_cent bigint NOT NULL CHECK (total_amount_cent > 0),
    currency varchar(8) NOT NULL DEFAULT 'CNY',
    status varchar(32) NOT NULL,
    aging_days integer NOT NULL DEFAULT 0 CHECK (aging_days >= 0),
    source_type varchar(64),
    source_id varchar(120),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT chk_merchant_bill_period CHECK (bill_period_end >= bill_period_start),
    CONSTRAINT chk_merchant_bill_status CHECK (status IN ('issued', 'viewed', 'confirmed', 'disputed', 'overdue', 'closed')),
    CONSTRAINT chk_merchant_bill_source_type CHECK (source_type IS NULL OR source_type IN ('ar_ap', 'manual', 'contract', 'meter', 'billing_batch'))
);

CREATE INDEX IF NOT EXISTS idx_merchant_bills_merchant_status_due
    ON merchant_bills (merchant_id, status, due_date, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_merchant_bills_ar_ap_item
    ON merchant_bills (ar_ap_item_id)
 WHERE ar_ap_item_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS merchant_bill_lines (
    id varchar(80) PRIMARY KEY,
    bill_id varchar(80) NOT NULL REFERENCES merchant_bills(id) ON DELETE CASCADE,
    fee_type varchar(40) NOT NULL,
    description varchar(500) NOT NULL,
    amount_cent bigint NOT NULL CHECK (amount_cent > 0),
    currency varchar(8) NOT NULL DEFAULT 'CNY',
    created_at timestamptz NOT NULL,
    CONSTRAINT chk_merchant_bill_line_fee_type CHECK (fee_type IN ('rent', 'property_fee', 'energy_fee', 'cam', 'deposit', 'other'))
);

CREATE INDEX IF NOT EXISTS idx_merchant_bill_lines_bill
    ON merchant_bill_lines (bill_id, created_at, id);

CREATE TABLE IF NOT EXISTS merchant_bill_disputes (
    id varchar(80) PRIMARY KEY,
    bill_id varchar(80) NOT NULL REFERENCES merchant_bills(id) ON DELETE CASCADE,
    merchant_id varchar(80) NOT NULL,
    reason_code varchar(80) NOT NULL,
    description varchar(2000) NOT NULL,
    status varchar(32) NOT NULL,
    evidence_attachment_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    operator_user_id varchar(80) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT chk_merchant_bill_dispute_status CHECK (status IN ('submitted', 'accepted', 'rejected', 'withdrawn'))
);

CREATE INDEX IF NOT EXISTS idx_merchant_bill_disputes_bill_status
    ON merchant_bill_disputes (bill_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS merchant_bill_dunning_records (
    id varchar(80) PRIMARY KEY,
    bill_id varchar(80) NOT NULL REFERENCES merchant_bills(id) ON DELETE CASCADE,
    merchant_id varchar(80) NOT NULL,
    dunning_channel varchar(32) NOT NULL,
    message_template varchar(160) NOT NULL,
    status varchar(32) NOT NULL,
    sent_at timestamptz NOT NULL,
    operator_user_id varchar(80) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT chk_merchant_bill_dunning_channel CHECK (dunning_channel IN ('wecom', 'sms', 'email', 'manual')),
    CONSTRAINT chk_merchant_bill_dunning_status CHECK (status IN ('sent', 'failed', 'scheduled'))
);

CREATE INDEX IF NOT EXISTS idx_merchant_bill_dunning_bill_created
    ON merchant_bill_dunning_records (bill_id, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('merchant.billing.view', 'View merchant bills', 'merchant_billing', 'medium', FALSE, 'active'),
    ('merchant.billing.manage', 'Create and manage merchant bills', 'merchant_billing', 'high', TRUE, 'active'),
    ('merchant.billing.confirm', 'Confirm merchant bills', 'merchant_billing', 'medium', FALSE, 'active'),
    ('merchant.billing.dispute.submit', 'Submit merchant bill disputes', 'merchant_billing', 'medium', FALSE, 'active'),
    ('merchant.billing.dunning.record', 'Record merchant bill dunning actions', 'merchant_billing', 'high', TRUE, 'active')
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
        'merchant.billing.view',
        'merchant.billing.manage',
        'merchant.billing.confirm',
        'merchant.billing.dispute.submit',
        'merchant.billing.dunning.record'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

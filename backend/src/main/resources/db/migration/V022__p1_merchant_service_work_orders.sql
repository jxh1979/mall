CREATE TABLE IF NOT EXISTS merchant_service_work_orders (
    id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    space_id VARCHAR(64),
    contract_id VARCHAR(64),
    work_order_type VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    reported_by_user_id VARCHAR(64) NOT NULL,
    reported_channel VARCHAR(64) NOT NULL,
    owner_department VARCHAR(64),
    owner_user_id VARCHAR(64),
    due_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    resolution VARCHAR(2000),
    satisfaction_score INTEGER,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_merchant_service_work_order_type CHECK (work_order_type IN ('repair', 'complaint', 'rectification', 'service')),
    CONSTRAINT chk_merchant_service_work_order_priority CHECK (priority IN ('low', 'medium', 'high', 'critical')),
    CONSTRAINT chk_merchant_service_work_order_status CHECK (status IN ('created', 'assigned', 'processing', 'pending_merchant', 'resolved', 'closed', 'cancelled')),
    CONSTRAINT chk_merchant_service_satisfaction_score CHECK (satisfaction_score IS NULL OR satisfaction_score BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_merchant_service_work_orders_merchant_status
    ON merchant_service_work_orders (merchant_id, status, priority, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_merchant_service_work_orders_owner
    ON merchant_service_work_orders (owner_department, owner_user_id, status)
 WHERE owner_department IS NOT NULL OR owner_user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_merchant_service_work_orders_due
    ON merchant_service_work_orders (due_at, status)
 WHERE due_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS merchant_service_work_order_actions (
    id VARCHAR(64) PRIMARY KEY,
    work_order_id VARCHAR(64) NOT NULL REFERENCES merchant_service_work_orders(id) ON DELETE CASCADE,
    action_type VARCHAR(32) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    operator_user_id VARCHAR(64) NOT NULL,
    operator_note VARCHAR(1000) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_merchant_service_work_order_action_type CHECK (action_type IN ('created', 'assigned', 'commented', 'status_changed', 'closed'))
);

CREATE INDEX IF NOT EXISTS idx_merchant_service_work_order_actions_order
    ON merchant_service_work_order_actions (work_order_id, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('merchant.service_work_order.view', 'View merchant service work orders', 'merchant_service', 'medium', FALSE, 'active'),
    ('merchant.service_work_order.create', 'Create merchant service work orders', 'merchant_service', 'high', TRUE, 'active'),
    ('merchant.service_work_order.assign', 'Assign merchant service work orders', 'merchant_service', 'high', TRUE, 'active'),
    ('merchant.service_work_order.process', 'Process merchant service work orders', 'merchant_service', 'high', TRUE, 'active'),
    ('merchant.service_work_order.close', 'Close merchant service work orders', 'merchant_service', 'high', TRUE, 'active')
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
        'merchant.service_work_order.view',
        'merchant.service_work_order.create',
        'merchant.service_work_order.assign',
        'merchant.service_work_order.process',
        'merchant.service_work_order.close'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

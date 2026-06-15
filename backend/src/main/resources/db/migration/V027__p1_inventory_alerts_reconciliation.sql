CREATE TABLE IF NOT EXISTS inventory_stock_batches (
    id varchar(80) PRIMARY KEY,
    item_id varchar(80) NOT NULL REFERENCES inventory_items(id),
    location_id varchar(80) NOT NULL,
    batch_no varchar(120) NOT NULL,
    supplier_id varchar(80) REFERENCES inventory_suppliers(id),
    quantity integer NOT NULL CHECK (quantity > 0),
    expire_at timestamptz,
    status varchar(32) NOT NULL,
    evidence_attachment_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT chk_inventory_stock_batch_status CHECK (status IN ('active', 'consumed', 'expired', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_batches_item_location
    ON inventory_stock_batches (item_id, location_id, status);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_batches_expire_at
    ON inventory_stock_batches (expire_at, status)
 WHERE expire_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS inventory_risk_alerts (
    id varchar(80) PRIMARY KEY,
    alert_type varchar(40) NOT NULL,
    severity varchar(16) NOT NULL,
    item_id varchar(80) NOT NULL,
    location_id varchar(80),
    batch_id varchar(80) REFERENCES inventory_stock_batches(id) ON DELETE SET NULL,
    available_quantity integer,
    threshold_quantity integer,
    expire_at timestamptz,
    status varchar(32) NOT NULL,
    operator_user_id varchar(80) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT chk_inventory_risk_alert_type CHECK (alert_type IN ('safety_stock_low', 'out_of_stock', 'expiry_warning')),
    CONSTRAINT chk_inventory_risk_alert_severity CHECK (severity IN ('low', 'medium', 'high', 'critical')),
    CONSTRAINT chk_inventory_risk_alert_status CHECK (status IN ('open', 'acknowledged', 'resolved', 'ignored'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_risk_alerts_status_type
    ON inventory_risk_alerts (status, alert_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_risk_alerts_item_location
    ON inventory_risk_alerts (item_id, location_id, created_at DESC);

CREATE TABLE IF NOT EXISTS inventory_supplier_reconciliations (
    id varchar(80) PRIMARY KEY,
    supplier_id varchar(80) NOT NULL REFERENCES inventory_suppliers(id),
    period_start date NOT NULL,
    period_end date NOT NULL,
    expected_amount_cent bigint NOT NULL CHECK (expected_amount_cent >= 0),
    actual_amount_cent bigint NOT NULL CHECK (actual_amount_cent >= 0),
    difference_amount_cent bigint NOT NULL,
    status varchar(32) NOT NULL,
    evidence_attachment_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    operator_user_id varchar(80) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT chk_inventory_supplier_reconciliation_period CHECK (period_end >= period_start),
    CONSTRAINT chk_inventory_supplier_reconciliation_status CHECK (status IN ('pending', 'matched', 'difference_found'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_supplier_reconciliations_supplier_period
    ON inventory_supplier_reconciliations (supplier_id, period_end DESC, status);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('inventory.stock.batch.manage', 'Manage inventory stock batches', 'inventory', 'high', TRUE, 'active'),
    ('inventory.risk_alert.view', 'View inventory risk alerts', 'inventory', 'medium', FALSE, 'active'),
    ('inventory.risk.scan', 'Run inventory risk scan', 'inventory', 'high', TRUE, 'active'),
    ('inventory.supplier.reconcile', 'Manage supplier reconciliations', 'inventory', 'high', TRUE, 'active')
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
        'inventory.stock.batch.manage',
        'inventory.risk_alert.view',
        'inventory.risk.scan',
        'inventory.supplier.reconcile'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

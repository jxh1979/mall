CREATE TABLE IF NOT EXISTS inventory_items (
    id VARCHAR(64) PRIMARY KEY,
    sku_code VARCHAR(128),
    item_name VARCHAR(128) NOT NULL,
    item_type VARCHAR(64) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    barcode VARCHAR(128),
    trace_required BOOLEAN NOT NULL DEFAULT FALSE,
    min_quantity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_inventory_items_min_quantity CHECK (min_quantity >= 0),
    CONSTRAINT chk_inventory_items_status CHECK (status IN ('active', 'disabled', 'paused', 'archived'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_items_sku_code
    ON inventory_items (sku_code)
 WHERE sku_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_inventory_items_status
    ON inventory_items (status, created_at DESC);

CREATE TABLE IF NOT EXISTS inventory_suppliers (
    id VARCHAR(64) PRIMARY KEY,
    supplier_name VARCHAR(120) NOT NULL,
    supplier_type VARCHAR(64) NOT NULL DEFAULT 'general',
    contact_name VARCHAR(120),
    contact_phone VARCHAR(20),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_inventory_suppliers_status CHECK (status IN ('active', 'paused', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_suppliers_status
    ON inventory_suppliers (status, created_at DESC);

CREATE TABLE IF NOT EXISTS inventory_stock_balances (
    id VARCHAR(64) PRIMARY KEY,
    item_id VARCHAR(64) NOT NULL REFERENCES inventory_items(id),
    location_id VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    available_quantity INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_inventory_stock_balances_item_location UNIQUE (item_id, location_id),
    CONSTRAINT chk_inventory_stock_balances_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_stock_balances_reserved CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inventory_stock_balances_available CHECK (available_quantity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_balances_location
    ON inventory_stock_balances (location_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS inventory_stock_movements (
    id VARCHAR(64) PRIMARY KEY,
    movement_type VARCHAR(64) NOT NULL,
    item_id VARCHAR(64) NOT NULL REFERENCES inventory_items(id),
    source_location_id VARCHAR(64),
    target_location_id VARCHAR(64),
    quantity INTEGER NOT NULL,
    business_type VARCHAR(64),
    business_id VARCHAR(64),
    trace_code VARCHAR(128),
    operator_id VARCHAR(128),
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_inventory_stock_movements_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_movements_item
    ON inventory_stock_movements (item_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_movements_trace
    ON inventory_stock_movements (trace_code)
 WHERE trace_code IS NOT NULL;

CREATE TABLE IF NOT EXISTS inventory_stock_adjustments (
    id VARCHAR(64) PRIMARY KEY,
    item_id VARCHAR(64) NOT NULL REFERENCES inventory_items(id),
    location_id VARCHAR(64) NOT NULL,
    adjustment_type VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL,
    reason TEXT NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_inventory_stock_adjustments_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_adjustments_item
    ON inventory_stock_adjustments (item_id, created_at DESC);

CREATE TABLE IF NOT EXISTS inventory_purchase_requests (
    id VARCHAR(64) PRIMARY KEY,
    request_no VARCHAR(64) NOT NULL UNIQUE,
    supplier_id VARCHAR(64) REFERENCES inventory_suppliers(id),
    item_id VARCHAR(64) NOT NULL REFERENCES inventory_items(id),
    quantity INTEGER NOT NULL,
    expected_arrival_date DATE,
    status VARCHAR(32) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_inventory_purchase_requests_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_inventory_purchase_requests_supplier
    ON inventory_purchase_requests (supplier_id, created_at DESC);

CREATE TABLE IF NOT EXISTS saved_bottles (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    bar_project_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(64) NOT NULL REFERENCES inventory_items(id),
    bottle_name VARCHAR(128) NOT NULL,
    initial_quantity INTEGER NOT NULL,
    remaining_quantity INTEGER NOT NULL,
    unit VARCHAR(32) NOT NULL,
    expire_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_saved_bottles_initial_quantity CHECK (initial_quantity > 0),
    CONSTRAINT chk_saved_bottles_remaining_quantity CHECK (remaining_quantity >= 0),
    CONSTRAINT chk_saved_bottles_status CHECK (status IN ('stored', 'claimed', 'cancelled', 'expired'))
);

CREATE INDEX IF NOT EXISTS idx_saved_bottles_customer
    ON saved_bottles (customer_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS saved_bottle_claims (
    id VARCHAR(64) PRIMARY KEY,
    saved_bottle_id VARCHAR(64) NOT NULL REFERENCES saved_bottles(id),
    claim_quantity INTEGER NOT NULL,
    claim_staff_id VARCHAR(128),
    claim_terminal_type VARCHAR(64),
    claim_device_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_saved_bottle_claims_quantity CHECK (claim_quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_saved_bottle_claims_saved_bottle
    ON saved_bottle_claims (saved_bottle_id, created_at DESC);

CREATE TABLE IF NOT EXISTS inventory_trace_handovers (
    id VARCHAR(64) PRIMARY KEY,
    trace_code VARCHAR(128) NOT NULL,
    item_id VARCHAR(64) REFERENCES inventory_items(id),
    from_staff_id VARCHAR(128),
    to_staff_id VARCHAR(128),
    device_id VARCHAR(128),
    handover_result VARCHAR(64) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_inventory_trace_handovers_trace
    ON inventory_trace_handovers (trace_code, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('inventory.stock.view', 'View inventory stock', 'inventory', 'medium', FALSE, 'active'),
    ('inventory.stock.move', 'Move inventory stock', 'inventory', 'high', TRUE, 'active'),
    ('inventory.stock.adjust', 'Adjust inventory stock', 'inventory', 'high', TRUE, 'active'),
    ('inventory.purchase_request.view', 'View inventory purchase requests', 'inventory', 'medium', FALSE, 'active'),
    ('inventory.purchase_request.manage', 'Manage inventory purchase requests', 'inventory', 'high', TRUE, 'active'),
    ('inventory.supplier.view', 'View inventory suppliers', 'inventory', 'medium', FALSE, 'active'),
    ('inventory.supplier.manage', 'Manage inventory suppliers', 'inventory', 'high', TRUE, 'active'),
    ('bar.saved_bottle.manage', 'Manage saved bottles', 'bar', 'high', TRUE, 'active')
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
        'inventory.stock.view',
        'inventory.stock.move',
        'inventory.stock.adjust',
        'inventory.purchase_request.view',
        'inventory.purchase_request.manage',
        'inventory.supplier.view',
        'inventory.supplier.manage',
        'bar.saved_bottle.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

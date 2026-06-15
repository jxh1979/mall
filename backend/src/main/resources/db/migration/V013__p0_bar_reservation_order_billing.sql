CREATE TABLE IF NOT EXISTS bar_tables (
    id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL,
    table_code VARCHAR(64) NOT NULL,
    table_name VARCHAR(120) NOT NULL,
    table_type VARCHAR(64) NOT NULL,
    capacity INTEGER NOT NULL DEFAULT 0,
    allowed_pass_free_guest_count INTEGER NOT NULL DEFAULT 0,
    min_consumption_cent BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_bar_tables_bar_code UNIQUE (bar_id, table_code),
    CONSTRAINT chk_bar_tables_capacity CHECK (capacity >= 0),
    CONSTRAINT chk_bar_tables_free_guest_count CHECK (allowed_pass_free_guest_count >= 0),
    CONSTRAINT chk_bar_tables_min_consumption CHECK (min_consumption_cent >= 0),
    CONSTRAINT chk_bar_tables_currency CHECK (currency = 'CNY')
);

CREATE INDEX IF NOT EXISTS idx_bar_tables_bar_status
    ON bar_tables (bar_id, status, table_type);

CREATE TABLE IF NOT EXISTS bar_reservations (
    id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL,
    table_id VARCHAR(64) NOT NULL REFERENCES bar_tables(id),
    customer_id VARCHAR(64),
    reserved_at TIMESTAMPTZ NOT NULL,
    guest_count INTEGER NOT NULL,
    allowed_pass_free_guest_count INTEGER NOT NULL DEFAULT 0,
    deposit_amount_cent BIGINT NOT NULL DEFAULT 0,
    deposit_status VARCHAR(32) NOT NULL,
    order_source VARCHAR(64) NOT NULL,
    sales_staff_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    created_terminal VARCHAR(64),
    device_id VARCHAR(128),
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_reservations_guest_count CHECK (guest_count >= 0),
    CONSTRAINT chk_bar_reservations_free_guest_count CHECK (allowed_pass_free_guest_count >= 0),
    CONSTRAINT chk_bar_reservations_deposit CHECK (deposit_amount_cent >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bar_reservations_bar_status
    ON bar_reservations (bar_id, status, reserved_at DESC);

CREATE INDEX IF NOT EXISTS idx_bar_reservations_customer
    ON bar_reservations (customer_id, reserved_at DESC)
 WHERE customer_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS bar_table_guest_checkins (
    id VARCHAR(64) PRIMARY KEY,
    reservation_id VARCHAR(64) NOT NULL REFERENCES bar_reservations(id) ON DELETE CASCADE,
    bar_id VARCHAR(64) NOT NULL,
    table_id VARCHAR(64) NOT NULL REFERENCES bar_tables(id),
    guest_identifier VARCHAR(255) NOT NULL,
    verification_result VARCHAR(64) NOT NULL,
    checked_in_count_after INTEGER NOT NULL,
    device_id VARCHAR(128),
    staff_id VARCHAR(128),
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_table_guest_checkins_count CHECK (checked_in_count_after >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bar_table_guest_checkins_reservation
    ON bar_table_guest_checkins (reservation_id, created_at DESC);

CREATE TABLE IF NOT EXISTS bar_age_verifications (
    id VARCHAR(64) PRIMARY KEY,
    business_id VARCHAR(64) NOT NULL,
    credential_payload VARCHAR(255) NOT NULL,
    device_id VARCHAR(128),
    staff_id VARCHAR(128),
    result VARCHAR(64) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    review_status VARCHAR(64) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bar_age_verifications_business
    ON bar_age_verifications (business_id, created_at DESC);

CREATE TABLE IF NOT EXISTS bar_products (
    id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    product_type VARCHAR(64) NOT NULL,
    price_cent BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    inventory_item_id VARCHAR(64),
    stock_location_id VARCHAR(64),
    stock_deduction_required BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_products_price CHECK (price_cent >= 0),
    CONSTRAINT chk_bar_products_currency CHECK (currency = 'CNY')
);

CREATE INDEX IF NOT EXISTS idx_bar_products_bar_status
    ON bar_products (bar_id, status, product_type);

CREATE TABLE IF NOT EXISTS bar_orders (
    id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL,
    table_id VARCHAR(64) NOT NULL REFERENCES bar_tables(id),
    reservation_id VARCHAR(64) REFERENCES bar_reservations(id),
    customer_id VARCHAR(64),
    order_source VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount_cent BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    created_terminal VARCHAR(64),
    device_id VARCHAR(128),
    sales_staff_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_orders_total CHECK (total_amount_cent >= 0),
    CONSTRAINT chk_bar_orders_currency CHECK (currency = 'CNY')
);

CREATE INDEX IF NOT EXISTS idx_bar_orders_bar_status
    ON bar_orders (bar_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS bar_order_items (
    id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL REFERENCES bar_orders(id) ON DELETE CASCADE,
    product_id VARCHAR(64) NOT NULL REFERENCES bar_products(id),
    product_name VARCHAR(128) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price_cent BIGINT NOT NULL,
    amount_cent BIGINT NOT NULL,
    delivery_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_bar_order_items_unit_price CHECK (unit_price_cent >= 0),
    CONSTRAINT chk_bar_order_items_amount CHECK (amount_cent >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bar_order_items_order
    ON bar_order_items (order_id);

CREATE TABLE IF NOT EXISTS bar_production_tickets (
    id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL REFERENCES bar_orders(id) ON DELETE CASCADE,
    bar_id VARCHAR(64) NOT NULL,
    ticket_status VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bar_production_tickets_order
    ON bar_production_tickets (order_id);

CREATE TABLE IF NOT EXISTS bar_bills (
    id VARCHAR(64) PRIMARY KEY,
    bill_no VARCHAR(64) NOT NULL UNIQUE,
    order_id VARCHAR(64) NOT NULL REFERENCES bar_orders(id),
    bar_id VARCHAR(64) NOT NULL,
    table_id VARCHAR(64) NOT NULL REFERENCES bar_tables(id),
    reservation_id VARCHAR(64) REFERENCES bar_reservations(id),
    subtotal_amount_cent BIGINT NOT NULL,
    discount_amount_cent BIGINT NOT NULL DEFAULT 0,
    deposit_deduct_amount_cent BIGINT NOT NULL DEFAULT 0,
    payable_amount_cent BIGINT NOT NULL,
    paid_amount_cent BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    payment_status VARCHAR(32) NOT NULL,
    settlement_status VARCHAR(32) NOT NULL,
    sales_staff_id VARCHAR(64),
    sales_role_type VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_bills_subtotal CHECK (subtotal_amount_cent >= 0),
    CONSTRAINT chk_bar_bills_discount CHECK (discount_amount_cent >= 0),
    CONSTRAINT chk_bar_bills_deposit_deduct CHECK (deposit_deduct_amount_cent >= 0),
    CONSTRAINT chk_bar_bills_payable CHECK (payable_amount_cent >= 0),
    CONSTRAINT chk_bar_bills_paid CHECK (paid_amount_cent >= 0),
    CONSTRAINT chk_bar_bills_currency CHECK (currency = 'CNY')
);

CREATE INDEX IF NOT EXISTS idx_bar_bills_order
    ON bar_bills (order_id);

CREATE TABLE IF NOT EXISTS bar_commission_calculations (
    id VARCHAR(64) PRIMARY KEY,
    bill_id VARCHAR(64) NOT NULL REFERENCES bar_bills(id) ON DELETE CASCADE,
    sales_staff_id VARCHAR(64),
    sales_role_type VARCHAR(64) NOT NULL,
    commission_base_cent BIGINT NOT NULL,
    commission_rate_bp INTEGER NOT NULL,
    commission_amount_cent BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_commission_base CHECK (commission_base_cent >= 0),
    CONSTRAINT chk_bar_commission_rate CHECK (commission_rate_bp >= 0),
    CONSTRAINT chk_bar_commission_amount CHECK (commission_amount_cent >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bar_commission_calculations_bill
    ON bar_commission_calculations (bill_id, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.entry.age_verify', 'Verify bar guest age and face boundary', 'bar', 'high', TRUE, 'active'),
    ('bar.table_commission.calculate', 'Calculate bar table commission', 'bar', 'high', TRUE, 'active'),
    ('bar.order.view', 'View bar orders', 'bar', 'medium', FALSE, 'active'),
    ('bar.order.create', 'Create and update bar orders', 'bar', 'high', TRUE, 'active'),
    ('bar.reservation.view', 'View bar reservations', 'bar', 'medium', FALSE, 'active'),
    ('bar.reservation.create', 'Create bar reservations', 'bar', 'high', TRUE, 'active'),
    ('bar.table.guest_checkin', 'Check in bar table free-entry guests', 'bar', 'high', TRUE, 'active'),
    ('bar.table.view', 'View bar tables', 'bar', 'medium', FALSE, 'active'),
    ('bar.table.manage', 'Manage bar tables', 'bar', 'high', TRUE, 'active'),
    ('bar.table.open_close', 'Open and close bar tables', 'bar', 'high', TRUE, 'active')
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
        'bar.entry.age_verify',
        'bar.table_commission.calculate',
        'bar.order.view',
        'bar.order.create',
        'bar.reservation.view',
        'bar.reservation.create',
        'bar.table.guest_checkin',
        'bar.table.view',
        'bar.table.manage',
        'bar.table.open_close'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

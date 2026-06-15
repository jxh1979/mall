CREATE TABLE IF NOT EXISTS customer_points_ledger (
    id varchar(80) PRIMARY KEY,
    customer_id varchar(80) NOT NULL REFERENCES customers(id),
    points_delta bigint NOT NULL,
    balance_after bigint NOT NULL CHECK (balance_after >= 0),
    source_type varchar(64) NOT NULL,
    source_id varchar(120),
    description text,
    created_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customer_points_ledger_customer_created
    ON customer_points_ledger (customer_id, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS customer_redemption_catalog_items (
    id varchar(80) PRIMARY KEY,
    item_name varchar(160) NOT NULL,
    item_type varchar(40) NOT NULL,
    points_cost bigint NOT NULL CHECK (points_cost > 0),
    inventory_item_id varchar(80) NOT NULL,
    stock_location_id varchar(80) NOT NULL,
    stock_quantity integer NOT NULL CHECK (stock_quantity > 0),
    status varchar(32) NOT NULL CHECK (status IN ('active', 'inactive', 'archived')),
    available_from timestamptz,
    available_until timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT chk_customer_redemption_catalog_window
        CHECK (available_until IS NULL OR available_from IS NULL OR available_until > available_from)
);

CREATE INDEX IF NOT EXISTS idx_customer_redemption_catalog_active
    ON customer_redemption_catalog_items (status, points_cost, item_name, id);

CREATE TABLE IF NOT EXISTS customer_points_redemptions (
    id varchar(80) PRIMARY KEY,
    customer_id varchar(80) NOT NULL REFERENCES customers(id),
    catalog_item_id varchar(80) NOT NULL REFERENCES customer_redemption_catalog_items(id),
    item_name varchar(160) NOT NULL,
    quantity integer NOT NULL CHECK (quantity > 0),
    points_cost bigint NOT NULL CHECK (points_cost > 0),
    total_points bigint NOT NULL CHECK (total_points > 0),
    inventory_item_id varchar(80) NOT NULL,
    stock_location_id varchar(80) NOT NULL,
    stock_quantity integer NOT NULL CHECK (stock_quantity > 0),
    source_terminal varchar(32) NOT NULL,
    status varchar(32) NOT NULL CHECK (status IN ('pending_pickup', 'picked_up', 'cancelled')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customer_points_redemptions_customer_created
    ON customer_points_redemptions (customer_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_customer_points_redemptions_status
    ON customer_points_redemptions (status, created_at DESC);

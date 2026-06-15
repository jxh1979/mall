CREATE TABLE IF NOT EXISTS ticket_price_rules (
    id VARCHAR(64) PRIMARY KEY,
    ticket_type_id VARCHAR(64) NOT NULL REFERENCES ticket_types(id),
    variant_type VARCHAR(64) NOT NULL,
    price_mode VARCHAR(64) NOT NULL,
    weekday_price_cent BIGINT,
    weekend_price_cent BIGINT,
    holiday_price_cent BIGINT,
    discount_rate NUMERIC(6, 2),
    channel_id VARCHAR(64),
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    status VARCHAR(32) NOT NULL,
    eligibility_rule JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ticket_price_rules_currency CHECK (currency = 'CNY')
);

CREATE INDEX IF NOT EXISTS idx_ticket_price_rules_ticket_type
    ON ticket_price_rules (ticket_type_id, status);

CREATE TABLE IF NOT EXISTS ticket_orders (
    id VARCHAR(64) PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    customer_id VARCHAR(64),
    ticket_type_id VARCHAR(64) NOT NULL REFERENCES ticket_types(id),
    price_rule_id VARCHAR(64) REFERENCES ticket_price_rules(id),
    quantity INTEGER NOT NULL,
    visit_date DATE,
    sales_channel VARCHAR(64) NOT NULL,
    buyer_phone VARCHAR(20),
    amount_cent BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    pay_status VARCHAR(32) NOT NULL,
    issue_status VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ticket_orders_quantity CHECK (quantity > 0),
    CONSTRAINT chk_ticket_orders_amount CHECK (amount_cent >= 0),
    CONSTRAINT chk_ticket_orders_currency CHECK (currency = 'CNY')
);

CREATE INDEX IF NOT EXISTS idx_ticket_orders_customer_created
    ON ticket_orders (customer_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ticket_orders_ticket_type
    ON ticket_orders (ticket_type_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ticket_order_items (
    id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL REFERENCES ticket_orders(id) ON DELETE CASCADE,
    ticket_type_id VARCHAR(64) NOT NULL REFERENCES ticket_types(id),
    issued_entitlement_id VARCHAR(64),
    quantity INTEGER NOT NULL,
    amount_cent BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ticket_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_ticket_order_items_currency CHECK (currency = 'CNY')
);

CREATE INDEX IF NOT EXISTS idx_ticket_order_items_order
    ON ticket_order_items (order_id);

CREATE TABLE IF NOT EXISTS ticket_payment_intents (
    id VARCHAR(64) PRIMARY KEY,
    business_order_type VARCHAR(64) NOT NULL,
    business_order_id VARCHAR(64) NOT NULL,
    amount_cent BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    channel VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ticket_payment_intents_amount CHECK (amount_cent > 0),
    CONSTRAINT chk_ticket_payment_intents_currency CHECK (currency = 'CNY')
);

CREATE INDEX IF NOT EXISTS idx_ticket_payment_intents_business_order
    ON ticket_payment_intents (business_order_type, business_order_id);

CREATE TABLE IF NOT EXISTS ticket_credentials (
    id VARCHAR(64) PRIMARY KEY,
    entitlement_id VARCHAR(64) NOT NULL REFERENCES issued_entitlements(id),
    order_id VARCHAR(64) NOT NULL,
    credential_token VARCHAR(128) NOT NULL UNIQUE,
    nonce VARCHAR(128) NOT NULL,
    credential_hash VARCHAR(128) NOT NULL,
    credential_type VARCHAR(64) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    refreshed_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_credentials_entitlement
    ON ticket_credentials (entitlement_id, issued_at DESC);

CREATE INDEX IF NOT EXISTS idx_ticket_credentials_token_status
    ON ticket_credentials (credential_token, status);

CREATE TABLE IF NOT EXISTS ticket_refunds (
    id VARCHAR(64) PRIMARY KEY,
    target_id VARCHAR(64) NOT NULL,
    refund_amount_cent BIGINT NOT NULL,
    reason TEXT NOT NULL,
    refund_channel VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    approved_by VARCHAR(128),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ticket_refunds_amount CHECK (refund_amount_cent > 0)
);

CREATE INDEX IF NOT EXISTS idx_ticket_refunds_target
    ON ticket_refunds (target_id, created_at DESC);

CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(64) PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    masked_phone VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_sessions (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    customer_token VARCHAR(128) NOT NULL UNIQUE,
    scene VARCHAR(64),
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customer_sessions_token_status
    ON customer_sessions (customer_token, status);

CREATE TABLE IF NOT EXISTS customer_consents (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    consent_type VARCHAR(64) NOT NULL,
    accepted BOOLEAN NOT NULL,
    version VARCHAR(255),
    source_terminal VARCHAR(64),
    accepted_at TIMESTAMPTZ,
    withdrawn_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customer_consents_customer
    ON customer_consents (customer_id, consent_type, created_at DESC);

CREATE TABLE IF NOT EXISTS customer_points_accounts (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL UNIQUE REFERENCES customers(id) ON DELETE CASCADE,
    balance BIGINT NOT NULL DEFAULT 0,
    level VARCHAR(64) NOT NULL DEFAULT 'base',
    recent_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('ticket.price_variant.view', 'View ticket price variants', 'ticketing', 'low', FALSE, 'active'),
    ('ticket.price_variant.manage', 'Manage ticket price variants', 'ticketing', 'high', TRUE, 'active'),
    ('finance.payment_intent.create', 'Create payment intents', 'finance', 'high', TRUE, 'active'),
    ('ticket.ticket.issue', 'Issue ticket order entitlements', 'ticketing', 'high', TRUE, 'active'),
    ('ticket.verify.view', 'View issued ticket entitlements', 'ticketing', 'medium', FALSE, 'active'),
    ('ticket.credential.view', 'View ticket dynamic credentials', 'ticketing', 'medium', FALSE, 'active'),
    ('ticket.refund.request', 'Request ticket refunds', 'ticketing', 'high', TRUE, 'active'),
    ('ticket.refund.approve', 'Approve ticket refunds', 'ticketing', 'high', TRUE, 'active')
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
        'ticket.price_variant.view',
        'ticket.price_variant.manage',
        'finance.payment_intent.create',
        'ticket.ticket.issue',
        'ticket.verify.view',
        'ticket.credential.view',
        'ticket.refund.request',
        'ticket.refund.approve'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

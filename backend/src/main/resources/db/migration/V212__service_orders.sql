-- G批(2026-07-02 六口径拍板):服务点单——后台目录固定参考价;服务员代客下单并指定履约人;
-- 直付口径(税务):服务费客人现场直付履约人收款码,酒吧不经手不抽成,不进酒吧账单/平台净收入/
-- 薪资提成,系统记参考价仅作台账;履约前可取消,完成后不可退。
CREATE TABLE IF NOT EXISTS service_catalog_items (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    price_cent BIGINT NOT NULL,
    duration_minutes INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_svc_price CHECK (price_cent >= 0)
);

CREATE TABLE IF NOT EXISTS staff_payment_codes (
    user_id VARCHAR(64) PRIMARY KEY,
    qr_content VARCHAR(500) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS service_orders (
    id VARCHAR(64) PRIMARY KEY,
    catalog_item_id VARCHAR(64) NOT NULL,
    item_name VARCHAR(120) NOT NULL,
    price_cent BIGINT NOT NULL,
    bar_id VARCHAR(64),
    table_id VARCHAR(64),
    performer_user_id VARCHAR(64) NOT NULL,
    ordered_by VARCHAR(64),
    customer_note VARCHAR(255),
    status VARCHAR(24) NOT NULL DEFAULT 'pending_accept',
    cancel_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_svc_order_status CHECK (status IN ('pending_accept', 'accepted', 'completed', 'cancelled'))
);
CREATE INDEX IF NOT EXISTS idx_svc_orders_performer ON service_orders (performer_user_id, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('service.catalog.manage', 'Manage service catalog (dance etc.)',            'service', 'medium', FALSE, 'active'),
    ('service.order.create',   'Create service orders on behalf of guests',      'service', 'low',    FALSE, 'active'),
    ('service.order.perform',  'Accept/complete assigned service orders',        'service', 'low',    FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', p.code FROM (VALUES
    ('service.catalog.manage'), ('service.order.create'), ('service.order.perform')) AS p(code)
ON CONFLICT (role_id, permission_code) DO NOTHING;

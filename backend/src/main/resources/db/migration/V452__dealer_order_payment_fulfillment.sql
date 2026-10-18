-- V452 CG-11b 经销订单、付款与履约（BSM:1226-1234；ENUM:173 订单十五态、176 支付方式、177 库存预留、178 职责分离、179 取消与到账互斥、180 价格与税务快照）。
-- E-13（BSM:1231）：首付款截止未达门槛的订单取消、释放预留、已收款与迟到款进待退款、退款完成回到已取消。不回填存量（铁律 8）。

-- 合同付款条款与运费模板（BSM:1227「每份经销合同约定首付款金额或比例、首付款截止时间及尾款到期规则」；BSM:1229「每份合同指定中心仓、运费模板、包邮门槛和承担方」）
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS credit_term BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS first_payment_ratio_bps INT CHECK (first_payment_ratio_bps IS NULL OR (first_payment_ratio_bps BETWEEN 1 AND 10000));
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS first_payment_amount_cent BIGINT CHECK (first_payment_amount_cent IS NULL OR first_payment_amount_cent > 0);
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS first_payment_deadline_days INT CHECK (first_payment_deadline_days IS NULL OR first_payment_deadline_days > 0);
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS balance_due_days INT CHECK (balance_due_days IS NULL OR balance_due_days > 0);
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS freight_cent BIGINT CHECK (freight_cent IS NULL OR freight_cent >= 0);
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS free_shipping_threshold_cent BIGINT CHECK (free_shipping_threshold_cent IS NULL OR free_shipping_threshold_cent >= 0);
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS freight_bearer VARCHAR(32) NOT NULL DEFAULT 'dealer' CHECK (freight_bearer IN ('dealer', 'sales_entity'));
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS freight_template_version INT NOT NULL DEFAULT 0;

-- BSM:1231「暂停该经销商新增账期订单，仍允许全款到账后发货的新订单。全部逾期货款结清后解除」
ALTER TABLE dealers ADD COLUMN IF NOT EXISTS credit_suspended BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS dealer_orders (
    id                         VARCHAR(64) PRIMARY KEY,
    order_no                   VARCHAR(64) NOT NULL UNIQUE,
    dealer_id                  VARCHAR(64) NOT NULL REFERENCES dealers(id),
    dealer_contract_id         VARCHAR(64) NOT NULL REFERENCES dealer_contracts(id),
    sales_entity_id            VARCHAR(64) NOT NULL,
    central_warehouse_id       VARCHAR(64) NOT NULL,
    -- ENUM:173 十五态
    status                     VARCHAR(32) NOT NULL DEFAULT 'draft',
    status_before_cancel       VARCHAR(32),
    price_catalog_id           VARCHAR(64) REFERENCES dealer_price_catalogs(id),
    credit_term                BOOLEAN     NOT NULL DEFAULT FALSE,
    goods_amount_cent          BIGINT      NOT NULL DEFAULT 0 CHECK (goods_amount_cent >= 0),
    freight_cent               BIGINT      NOT NULL DEFAULT 0 CHECK (freight_cent >= 0),
    freight_bearer             VARCHAR(32) NOT NULL DEFAULT 'dealer',
    freight_template_version   INT         NOT NULL DEFAULT 0,
    total_amount_cent          BIGINT      NOT NULL DEFAULT 0 CHECK (total_amount_cent >= 0),
    first_payment_required_cent BIGINT     NOT NULL DEFAULT 0 CHECK (first_payment_required_cent >= 0),
    first_payment_deadline_at  TIMESTAMPTZ,
    balance_due_at             TIMESTAMPTZ,
    paid_amount_cent           BIGINT      NOT NULL DEFAULT 0 CHECK (paid_amount_cent >= 0),
    overdue                    BOOLEAN     NOT NULL DEFAULT FALSE,
    ar_ap_item_id              VARCHAR(64),
    reviewed_by                VARCHAR(64),
    reviewed_at                TIMESTAMPTZ,
    payment_notified_at        TIMESTAMPTZ,
    shipped_by                 VARCHAR(64),
    shipped_at                 TIMESTAMPTZ,
    signed_at                  TIMESTAMPTZ,
    completed_at               TIMESTAMPTZ,
    cancel_requested_by        VARCHAR(64),
    cancel_reason              VARCHAR(1000),
    cancelled_at               TIMESTAMPTZ,
    note                       VARCHAR(1000),
    created_by                 VARCHAR(64),
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_dealer_orders_status CHECK (status IN (
        'draft', 'pending_review', 'rejected', 'pending_payment', 'payment_verifying', 'pending_shipment', 'shipping', 'signed', 'completed',
        'cancelling', 'cancelled', 'pending_refund', 'returning', 'partially_returned', 'returned'))
);
CREATE INDEX IF NOT EXISTS idx_dealer_orders_dealer ON dealer_orders (dealer_id, status);
CREATE INDEX IF NOT EXISTS idx_dealer_orders_deadline ON dealer_orders (first_payment_deadline_at) WHERE status IN ('pending_payment', 'payment_verifying');
CREATE INDEX IF NOT EXISTS idx_dealer_orders_balance_due ON dealer_orders (balance_due_at) WHERE status IN ('shipping', 'signed') AND overdue = FALSE;

CREATE TABLE IF NOT EXISTS dealer_order_lines (
    id               VARCHAR(64) PRIMARY KEY,
    order_id         VARCHAR(64) NOT NULL REFERENCES dealer_orders(id) ON DELETE CASCADE,
    item_id          VARCHAR(64) NOT NULL REFERENCES inventory_items(id),
    spec             VARCHAR(100) NOT NULL DEFAULT '',
    quantity         INT         NOT NULL CHECK (quantity > 0),
    unit_price_cent  BIGINT      NOT NULL DEFAULT 0 CHECK (unit_price_cent >= 0),
    -- ENUM:180 税率仅作审计快照
    tax_rate_bps     INT,
    -- 通知付款时固化：catalog（合同价格目录版本）/ inquiry（已报价询价单）
    price_source     VARCHAR(16),
    inquiry_id       VARCHAR(64) REFERENCES dealer_inquiries(id),
    line_amount_cent BIGINT      NOT NULL DEFAULT 0 CHECK (line_amount_cent >= 0),
    CONSTRAINT chk_dealer_order_lines_source CHECK (price_source IS NULL OR price_source IN ('catalog', 'inquiry'))
);
CREATE INDEX IF NOT EXISTS idx_dealer_order_lines_order ON dealer_order_lines (order_id);

-- ENUM:176「对公转账、平台线上支付；禁止现金和个人账户收款」
CREATE TABLE IF NOT EXISTS dealer_order_payments (
    id            VARCHAR(64) PRIMARY KEY,
    order_id      VARCHAR(64) NOT NULL REFERENCES dealer_orders(id),
    method        VARCHAR(32) NOT NULL,
    amount_cent   BIGINT      NOT NULL CHECK (amount_cent > 0),
    received_at   TIMESTAMPTZ NOT NULL,
    reference_no  VARCHAR(128),
    -- pending_verification 待核验 / verified 已核验 / rejected 核验不通过 / late 取消后迟到（进待退款）
    status        VARCHAR(32) NOT NULL DEFAULT 'pending_verification',
    applied_cent  BIGINT      NOT NULL DEFAULT 0 CHECK (applied_cent >= 0),
    registered_by VARCHAR(64),
    verified_by   VARCHAR(64),
    verified_at   TIMESTAMPTZ,
    note          VARCHAR(1000),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_dealer_order_payments_method CHECK (method IN ('bank_transfer', 'platform_online')),
    CONSTRAINT chk_dealer_order_payments_status CHECK (status IN ('pending_verification', 'verified', 'rejected', 'late'))
);
CREATE INDEX IF NOT EXISTS idx_dealer_order_payments_order ON dealer_order_payments (order_id, status);

-- E-13 退款：按既有退款审批（supervisor_review 的 refund 策略）原路退回；退款执行人与到账核验人不得为同一人（BSM:1232）
CREATE TABLE IF NOT EXISTS dealer_order_refunds (
    id            VARCHAR(64) PRIMARY KEY,
    order_id      VARCHAR(64) NOT NULL REFERENCES dealer_orders(id),
    payment_id    VARCHAR(64) NOT NULL REFERENCES dealer_order_payments(id),
    amount_cent   BIGINT      NOT NULL CHECK (amount_cent > 0),
    -- pending_request 待发起审批（系统取消、无操作人）/ pending_approval 审批中 / approved 已批准待执行 / rejected 已驳回 / refunded 已退款
    status        VARCHAR(32) NOT NULL DEFAULT 'pending_request',
    review_id     VARCHAR(64) UNIQUE,
    requested_by  VARCHAR(64),
    executed_by   VARCHAR(64),
    executed_at   TIMESTAMPTZ,
    reference_no  VARCHAR(128),
    reason        VARCHAR(1000),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_dealer_order_refunds_status CHECK (status IN ('pending_request', 'pending_approval', 'approved', 'rejected', 'refunded'))
);
CREATE INDEX IF NOT EXISTS idx_dealer_order_refunds_order ON dealer_order_refunds (order_id, status);

-- BSM:1228 超收：单独记录尚未确认用途的金额，不自动抵账；用途确认后转预付款或抵扣同一销售主体下指定订单，同一金额不得重复抵用
CREATE TABLE IF NOT EXISTS dealer_order_overpayments (
    id               VARCHAR(64) PRIMARY KEY,
    order_id         VARCHAR(64) NOT NULL REFERENCES dealer_orders(id),
    dealer_id        VARCHAR(64) NOT NULL REFERENCES dealers(id),
    sales_entity_id  VARCHAR(64) NOT NULL,
    payment_id       VARCHAR(64) NOT NULL REFERENCES dealer_order_payments(id),
    amount_cent      BIGINT      NOT NULL CHECK (amount_cent > 0),
    status           VARCHAR(32) NOT NULL DEFAULT 'unconfirmed',
    target_order_id  VARCHAR(64) REFERENCES dealer_orders(id),
    confirmed_by     VARCHAR(64),
    confirmed_at     TIMESTAMPTZ,
    note             VARCHAR(1000),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_dealer_order_overpayments_status CHECK (status IN ('unconfirmed', 'prepayment', 'offset_applied'))
);
CREATE INDEX IF NOT EXISTS idx_dealer_order_overpayments_dealer ON dealer_order_overpayments (dealer_id, status);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('dealer.order.view',            '查看经销订单、付款、退款与超收', 'dealer', 'medium', FALSE, 'active'),
    ('dealer.order.manage',          '经销订单建单、提交、登记付款、签收、取消申请', 'dealer', 'high', FALSE, 'active'),
    ('dealer.order.review',          '经销订单审核与取消审批（不得与到账核验、出库同人）', 'dealer', 'high', FALSE, 'active'),
    ('dealer.order.payment.verify',  '经销订单到账核验与超收用途确认（不得与审核、出库同人）', 'dealer', 'high', FALSE, 'active'),
    ('dealer.order.ship',            '经销订单仓库出库（不得与审核、到账核验同人）', 'dealer', 'high', FALSE, 'active'),
    ('dealer.order.refund.execute',  '经销订单退款执行（不得与到账核验同人）', 'dealer', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_platform_admin', 'dealer.order.view'),
    ('role_platform_admin', 'dealer.order.manage'),
    ('role_platform_admin', 'dealer.order.review'),
    ('role_platform_admin', 'dealer.order.payment.verify'),
    ('role_platform_admin', 'dealer.order.ship'),
    ('role_platform_admin', 'dealer.order.refund.execute')
ON CONFLICT DO NOTHING;

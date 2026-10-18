-- V453 CG-11c 经销退货、退款、红冲与资金归属（BSM:1238-1251；ENUM:174 退货单十态、178 退货验收与退款执行分离、179 取消/退货/开票、182 资金归属）。
-- 不回填存量（铁律 8）。

-- 合同约定：非质量退货的退回运费承担方、已收首程运费退不退（BSM:1240「按订单锁定的合同约定；未约定先由双方确认」）
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS return_freight_bearer VARCHAR(32) CHECK (return_freight_bearer IS NULL OR return_freight_bearer IN ('dealer', 'sales_entity'));
ALTER TABLE dealer_contracts ADD COLUMN IF NOT EXISTS first_leg_freight_refundable BOOLEAN;

-- 订单累计已退（验收确认）金额；明细累计已退数量（支持部分退货，数量不得超过已出库未退数量）
ALTER TABLE dealer_orders ADD COLUMN IF NOT EXISTS returned_amount_cent BIGINT NOT NULL DEFAULT 0 CHECK (returned_amount_cent >= 0);
ALTER TABLE dealer_order_lines ADD COLUMN IF NOT EXISTS returned_quantity INT NOT NULL DEFAULT 0 CHECK (returned_quantity >= 0);

-- ENUM:174「待审批、已驳回、待收货、验收中、待红冲、待退款、退款中、退款失败、部分完成、已完成」
CREATE TABLE IF NOT EXISTS dealer_returns (
    id                        VARCHAR(64) PRIMARY KEY,
    return_no                 VARCHAR(64) NOT NULL UNIQUE,
    order_id                  VARCHAR(64) NOT NULL REFERENCES dealer_orders(id),
    dealer_id                 VARCHAR(64) NOT NULL REFERENCES dealers(id),
    sales_entity_id           VARCHAR(64) NOT NULL,
    status                    VARCHAR(32) NOT NULL DEFAULT 'pending_approval',
    -- BSM:1240「仅未拆封可复售商品或质量问题商品可以申请退货」
    reason_type               VARCHAR(32) NOT NULL,
    requested_quantity        INT         NOT NULL DEFAULT 0 CHECK (requested_quantity >= 0),
    accepted_quantity         INT         NOT NULL DEFAULT 0 CHECK (accepted_quantity >= 0),
    accepted_amount_cent      BIGINT      NOT NULL DEFAULT 0 CHECK (accepted_amount_cent >= 0),
    offset_receivable_cent    BIGINT      NOT NULL DEFAULT 0 CHECK (offset_receivable_cent >= 0),
    refund_cent               BIGINT      NOT NULL DEFAULT 0 CHECK (refund_cent >= 0),
    refunded_cent             BIGINT      NOT NULL DEFAULT 0 CHECK (refunded_cent >= 0),
    refund_review_id          VARCHAR(64) UNIQUE,
    refund_approved           BOOLEAN     NOT NULL DEFAULT FALSE,
    -- 退回运费承担方：质量问题＝销售主体；非质量＝合同约定，未约定＝pending_confirmation（双方确认后填）
    return_freight_bearer     VARCHAR(32) NOT NULL DEFAULT 'pending_confirmation',
    first_leg_freight_refundable BOOLEAN,
    freight_confirmed         BOOLEAN     NOT NULL DEFAULT FALSE,
    approved_by               VARCHAR(64),
    approved_at               TIMESTAMPTZ,
    received_by               VARCHAR(64),
    received_at               TIMESTAMPTZ,
    inspected_by              VARCHAR(64),
    inspected_at              TIMESTAMPTZ,
    refund_executed_by        VARCHAR(64),
    refund_executed_at        TIMESTAMPTZ,
    refund_reference_no       VARCHAR(128),
    failure_reason            VARCHAR(1000),
    note                      VARCHAR(1000),
    created_by                VARCHAR(64),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_dealer_returns_status CHECK (status IN (
        'pending_approval', 'rejected', 'pending_receipt', 'inspecting', 'pending_reversal', 'pending_refund', 'refunding', 'refund_failed',
        'partially_completed', 'completed')),
    CONSTRAINT chk_dealer_returns_reason CHECK (reason_type IN ('unopened_resalable', 'quality_issue')),
    CONSTRAINT chk_dealer_returns_freight_bearer CHECK (return_freight_bearer IN ('dealer', 'sales_entity', 'pending_confirmation'))
);
CREATE INDEX IF NOT EXISTS idx_dealer_returns_order ON dealer_returns (order_id, status);

CREATE TABLE IF NOT EXISTS dealer_return_lines (
    id                   VARCHAR(64) PRIMARY KEY,
    return_id            VARCHAR(64) NOT NULL REFERENCES dealer_returns(id) ON DELETE CASCADE,
    order_line_id        VARCHAR(64) NOT NULL REFERENCES dealer_order_lines(id),
    item_id              VARCHAR(64) NOT NULL,
    spec                 VARCHAR(100) NOT NULL DEFAULT '',
    requested_quantity   INT         NOT NULL CHECK (requested_quantity > 0),
    accepted_quantity    INT         NOT NULL DEFAULT 0 CHECK (accepted_quantity >= 0),
    resalable_quantity   INT         NOT NULL DEFAULT 0 CHECK (resalable_quantity >= 0),
    quarantined_quantity INT         NOT NULL DEFAULT 0 CHECK (quarantined_quantity >= 0),
    -- BSM:1240「货款按原成交价和验收退货数量调整」
    unit_price_cent      BIGINT      NOT NULL CHECK (unit_price_cent >= 0),
    accepted_amount_cent BIGINT      NOT NULL DEFAULT 0 CHECK (accepted_amount_cent >= 0),
    inspection_note      VARCHAR(500),
    CONSTRAINT chk_dealer_return_lines_split CHECK (resalable_quantity + quarantined_quantity = accepted_quantity)
);
CREATE INDEX IF NOT EXISTS idx_dealer_return_lines_return ON dealer_return_lines (return_id);

-- 经销电子发票（BSM:1242「订单签收后才可按最终未退金额申请电子发票」「已开票后部分退货先全额红冲原发票，再按最新未退金额重开；全部退货红冲后不重开」）
-- 状态沿用 ENUM「开票状态」六态：pending 待开票 / issuing 开票中 / issued 已开票 / reversed 已红冲 / voided 已作废 / failed 开票失败
CREATE TABLE IF NOT EXISTS dealer_invoices (
    id                       VARCHAR(64) PRIMARY KEY,
    order_id                 VARCHAR(64) NOT NULL REFERENCES dealer_orders(id),
    dealer_id                VARCHAR(64) NOT NULL REFERENCES dealers(id),
    -- ENUM:182「合同指定销售主体负责经销货款收款和开票」
    sales_entity_id          VARCHAR(64) NOT NULL,
    amount_cent              BIGINT      NOT NULL CHECK (amount_cent > 0),
    tax_rate_bps             INT,
    status                   VARCHAR(24) NOT NULL DEFAULT 'pending',
    invoice_no               VARCHAR(64),
    reversed_by_return_id    VARCHAR(64) REFERENCES dealer_returns(id),
    reissued_from_invoice_id VARCHAR(64) REFERENCES dealer_invoices(id),
    requested_by             VARCHAR(64),
    requested_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    issued_by                VARCHAR(64),
    issued_at                TIMESTAMPTZ,
    reversed_at              TIMESTAMPTZ,
    note                     VARCHAR(500),
    CONSTRAINT chk_dealer_invoices_status CHECK (status IN ('pending', 'issuing', 'issued', 'reversed', 'voided', 'failed'))
);
CREATE INDEX IF NOT EXISTS idx_dealer_invoices_order ON dealer_invoices (order_id, status);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('dealer.return.view',           '查看经销退货单与发票', 'dealer', 'medium', FALSE, 'active'),
    ('dealer.return.manage',         '经销退货申请、退货运费确认', 'dealer', 'high', FALSE, 'active'),
    ('dealer.return.review',         '经销退货审批（不得与申请人同人）', 'dealer', 'high', FALSE, 'active'),
    ('dealer.return.inspect',        '经销退货收货与验收（不得执行退款）', 'dealer', 'high', FALSE, 'active'),
    ('dealer.return.refund.execute', '经销退货退款执行与结果登记（不得与验收人同人）', 'dealer', 'high', FALSE, 'active'),
    ('dealer.invoice.manage',        '经销电子发票申请、开具、红冲与重开', 'dealer', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_platform_admin', 'dealer.return.view'),
    ('role_platform_admin', 'dealer.return.manage'),
    ('role_platform_admin', 'dealer.return.review'),
    ('role_platform_admin', 'dealer.return.inspect'),
    ('role_platform_admin', 'dealer.return.refund.execute'),
    ('role_platform_admin', 'dealer.invoice.manage')
ON CONFLICT DO NOTHING;

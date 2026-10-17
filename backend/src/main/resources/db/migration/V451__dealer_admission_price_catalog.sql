-- V451 CG-11a 外部经销商准入、价格与库存（BSM:1216-1222；ENUM:170 经销商状态、171 询价状态、172 价格目录状态、175 库存外显、178 职责分离）。
-- 经销商与商户/供应商/外部协作机构隔离建档（BSM:1218「与商户门户及内部员工端隔离账号、权限和数据」）；
-- 每份经销合同一套可版本化的合同销售价格目录，不复用内部结算价（BSM:1219）；未定价商品只能询价（BSM:1220）；
-- 外部库存只按中心仓外显四态（BSM:1221）。状态码为技术决定，中文闭集逐字对应 ENUM 原文。不回填存量（铁律 8）。

CREATE TABLE IF NOT EXISTS dealers (
    id                        VARCHAR(64)  PRIMARY KEY,
    project_id                VARCHAR(64),
    dealer_name               VARCHAR(200) NOT NULL,
    unified_social_credit_code VARCHAR(64),
    legal_representative      VARCHAR(100),
    contact_name              VARCHAR(100),
    contact_phone             VARCHAR(40),
    -- ENUM:170「待提交、审核中、已通过、已驳回、已停用」
    status                    VARCHAR(32)  NOT NULL DEFAULT 'pending_submission',
    company_profile_reviewed  BOOLEAN      NOT NULL DEFAULT FALSE,
    contract_reviewed         BOOLEAN      NOT NULL DEFAULT FALSE,
    invoice_reviewed          BOOLEAN      NOT NULL DEFAULT FALSE,
    bank_reviewed             BOOLEAN      NOT NULL DEFAULT FALSE,
    -- BSM:1217「企业资料、合同、开票和银行资料审核通过后，才可启用独立经销商 Web 门户」
    portal_enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
    review_note               VARCHAR(1000),
    reviewed_by               VARCHAR(64),
    reviewed_at               TIMESTAMPTZ,
    deactivation_reason       VARCHAR(1000),
    deactivated_by            VARCHAR(64),
    deactivated_at            TIMESTAMPTZ,
    created_by                VARCHAR(64),
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_dealers_status CHECK (status IN ('pending_submission', 'under_review', 'approved', 'rejected', 'deactivated')),
    CONSTRAINT chk_dealers_portal_requires_approved CHECK (portal_enabled = FALSE OR status = 'approved')
);
CREATE INDEX IF NOT EXISTS idx_dealers_project_status ON dealers (project_id, status);

CREATE TABLE IF NOT EXISTS dealer_contracts (
    id                   VARCHAR(64) PRIMARY KEY,
    dealer_id            VARCHAR(64) NOT NULL REFERENCES dealers(id),
    contract_no          VARCHAR(64) NOT NULL UNIQUE,
    -- ENUM:182 / BSM:1248「每份合同指定销售主体」
    sales_entity_id      VARCHAR(64) NOT NULL REFERENCES business_entities(id),
    -- BSM:1229「每份合同指定中心仓」：库存外显（BSM:1221）按该中心仓计算
    central_warehouse_id VARCHAR(64) NOT NULL REFERENCES warehouses(id),
    valid_from           DATE        NOT NULL,
    valid_to             DATE        NOT NULL,
    status               VARCHAR(32) NOT NULL DEFAULT 'active',
    created_by           VARCHAR(64),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_dealer_contracts_status CHECK (status IN ('active', 'expired', 'terminated')),
    CONSTRAINT chk_dealer_contracts_period CHECK (valid_to >= valid_from)
);
CREATE INDEX IF NOT EXISTS idx_dealer_contracts_dealer ON dealer_contracts (dealer_id, status);

-- ENUM:172「草稿、待审批、已生效、已失效；版本只影响生效后的订单，不复用内部结算价」
CREATE TABLE IF NOT EXISTS dealer_price_catalogs (
    id                 VARCHAR(64) PRIMARY KEY,
    dealer_contract_id VARCHAR(64) NOT NULL REFERENCES dealer_contracts(id),
    version            INT         NOT NULL,
    status             VARCHAR(32) NOT NULL DEFAULT 'draft',
    edited_by          VARCHAR(64) NOT NULL,
    submitted_by       VARCHAR(64),
    submitted_at       TIMESTAMPTZ,
    approved_by        VARCHAR(64),
    approved_at        TIMESTAMPTZ,
    effective_at       TIMESTAMPTZ,
    expired_at         TIMESTAMPTZ,
    note               VARCHAR(1000),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_dealer_price_catalogs_status CHECK (status IN ('draft', 'pending_approval', 'effective', 'expired')),
    CONSTRAINT uq_dealer_price_catalogs_version UNIQUE (dealer_contract_id, version)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_dealer_price_catalogs_effective ON dealer_price_catalogs (dealer_contract_id) WHERE status = 'effective';

CREATE TABLE IF NOT EXISTS dealer_price_catalog_lines (
    id                   VARCHAR(64) PRIMARY KEY,
    catalog_id           VARCHAR(64) NOT NULL REFERENCES dealer_price_catalogs(id) ON DELETE CASCADE,
    item_id              VARCHAR(64) NOT NULL REFERENCES inventory_items(id),
    spec                 VARCHAR(100) NOT NULL DEFAULT '',
    unit_price_cent      BIGINT      NOT NULL CHECK (unit_price_cent > 0),
    -- ENUM:180 税率仅作审计快照
    tax_rate_bps         INT         CHECK (tax_rate_bps IS NULL OR (tax_rate_bps >= 0 AND tax_rate_bps <= 10000)),
    -- BSM:1221「预计交期」：缺货时对外显示的预计到货日
    expected_restock_date DATE,
    CONSTRAINT uq_dealer_price_catalog_lines UNIQUE (catalog_id, item_id, spec)
);

-- ENUM:171「待报价、报价审批中、已报价、已失效」
CREATE TABLE IF NOT EXISTS dealer_inquiries (
    id                 VARCHAR(64) PRIMARY KEY,
    dealer_id          VARCHAR(64) NOT NULL REFERENCES dealers(id),
    dealer_contract_id VARCHAR(64) NOT NULL REFERENCES dealer_contracts(id),
    item_id            VARCHAR(64) NOT NULL REFERENCES inventory_items(id),
    spec               VARCHAR(100) NOT NULL DEFAULT '',
    quantity           INT         NOT NULL CHECK (quantity > 0),
    status             VARCHAR(32) NOT NULL DEFAULT 'pending_quote',
    quoted_price_cent  BIGINT      CHECK (quoted_price_cent IS NULL OR quoted_price_cent > 0),
    quoted_by          VARCHAR(64),
    quoted_at          TIMESTAMPTZ,
    quote_valid_until  DATE,
    approved_by        VARCHAR(64),
    approved_at        TIMESTAMPTZ,
    note               VARCHAR(1000),
    created_by         VARCHAR(64),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_dealer_inquiries_status CHECK (status IN ('pending_quote', 'quote_approval', 'quoted', 'expired'))
);
CREATE INDEX IF NOT EXISTS idx_dealer_inquiries_dealer ON dealer_inquiries (dealer_id, status);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('dealer.view',                  '查看经销商、合同、价格目录与询价', 'dealer', 'medium', FALSE, 'active'),
    ('dealer.manage',                '经销商建档、提交审核、停用与合同登记', 'dealer', 'high', FALSE, 'active'),
    ('dealer.review',                '经销商准入审核（企业资料、合同、开票、银行资料）', 'dealer', 'high', FALSE, 'active'),
    ('dealer.price_catalog.edit',    '编辑并提交合同销售价格目录', 'dealer', 'high', FALSE, 'active'),
    ('dealer.price_catalog.approve', '审批合同销售价格目录（不得与编辑人同人）', 'dealer', 'high', FALSE, 'active'),
    ('dealer.inquiry.manage',        '登记经销询价', 'dealer', 'medium', FALSE, 'active'),
    ('dealer.inquiry.quote',         '对询价报价、置失效', 'dealer', 'high', FALSE, 'active'),
    ('dealer.inquiry.approve',       '审批询价报价（不得与报价人同人）', 'dealer', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_platform_admin', 'dealer.view'),
    ('role_platform_admin', 'dealer.manage'),
    ('role_platform_admin', 'dealer.review'),
    ('role_platform_admin', 'dealer.price_catalog.edit'),
    ('role_platform_admin', 'dealer.price_catalog.approve'),
    ('role_platform_admin', 'dealer.inquiry.manage'),
    ('role_platform_admin', 'dealer.inquiry.quote'),
    ('role_platform_admin', 'dealer.inquiry.approve')
ON CONFLICT DO NOTHING;

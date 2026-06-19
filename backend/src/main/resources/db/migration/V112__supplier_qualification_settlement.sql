-- P1 清单#18：供应商资质（进货查验）+ 结算多档/账期子表。
-- 背景：供应商主表 inventory_suppliers（V012）仅承载名称/联系人/单一 status；
--   ① 进货查验所需的资质（营业执照/食品经营许可/检验报告等：类型/编号/有效期/附件/状态）无处落库；
--   ② 多档结算/账期（不同结算方式 + 账期 + 适用品类 + 默认档）无处落库——主表既有 settlement_method 字段
--      保留为“兼容默认”，本迁移以子表承载多档结算。
-- 现状核对：供应链 T6 主表迁移 V085 supply_chain_t6（supply_contracts/warehouses/采购周期/高价值复核）；
--   供应商主表 inventory_suppliers 含 id/supplier_name/supplier_type/contact_name/contact_phone/status/
--   created_at/updated_at/merchant_id（无资质/结算子表）。本迁移仅建两张子表 + 权限 seed，不动主表。
-- 口径对齐：id VARCHAR(64)、隔离列 merchant_id（direct）、CHECK 收口枚举、JSONB 附件、TIMESTAMPTZ。

-- ── 1) 供应商资质（进货查验绑定） ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS supplier_qualifications (
    id VARCHAR(64) PRIMARY KEY,
    supplier_id VARCHAR(64) NOT NULL REFERENCES inventory_suppliers(id),
    qualification_type VARCHAR(48) NOT NULL,          -- 资质类型（营业执照/食品经营许可/检验报告等）
    qualification_no VARCHAR(128),                    -- 资质编号
    issued_by VARCHAR(128),                           -- 发证/出具机构
    valid_from DATE,
    valid_to DATE,                                    -- 有效期止（进货查验过期判定依据，可空=长期有效）
    attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,-- 资质扫描件/附件
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    merchant_id VARCHAR(64),                          -- 隔离（direct）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_supplier_qual_type CHECK (qualification_type IN (
        'business_license', 'food_business_license', 'production_license',
        'inspection_report', 'hygiene_permit', 'import_record', 'other')),
    CONSTRAINT chk_supplier_qual_status CHECK (status IN ('active', 'suspended', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_supplier_qualifications_supplier
    ON supplier_qualifications (supplier_id, valid_to);
CREATE INDEX IF NOT EXISTS idx_supplier_qualifications_merchant
    ON supplier_qualifications (merchant_id) WHERE merchant_id IS NOT NULL;

-- ── 2) 供应商结算多档/账期（主表 settlement_method 保留为兼容默认） ─────────────
CREATE TABLE IF NOT EXISTS supplier_settlement_methods (
    id VARCHAR(64) PRIMARY KEY,
    supplier_id VARCHAR(64) NOT NULL REFERENCES inventory_suppliers(id),
    settlement_method VARCHAR(40) NOT NULL,           -- 结算方式
    payment_term_days INTEGER,                        -- 账期（天，可空=即时/无账期）
    applicable_category VARCHAR(64),                  -- 适用品类（空=不限）
    is_default BOOLEAN NOT NULL DEFAULT FALSE,        -- 默认档
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    merchant_id VARCHAR(64),                          -- 隔离（direct）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_supplier_settle_method CHECK (settlement_method IN (
        'cash_on_delivery', 'monthly', 'credit_term', 'prepaid', 'consignment')),
    CONSTRAINT chk_supplier_settle_term_days CHECK (payment_term_days IS NULL OR payment_term_days >= 0),
    CONSTRAINT chk_supplier_settle_status CHECK (status IN ('active', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_supplier_settlement_methods_supplier
    ON supplier_settlement_methods (supplier_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_supplier_settlement_methods_merchant
    ON supplier_settlement_methods (merchant_id) WHERE merchant_id IS NOT NULL;

-- 每供应商最多一条默认结算档（部分唯一索引，仅约束 active 默认档）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_supplier_settlement_default
    ON supplier_settlement_methods (supplier_id)
    WHERE is_default = TRUE AND status = 'active';

-- ── 3) 权限点 seed（沿用既有 inventory.supplier.* 体系） ────────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('inventory.supplier.qualification.manage', 'Manage supplier qualifications', 'inventory', 'medium', FALSE, 'active'),
    ('inventory.supplier.settlement.manage',    'Manage supplier settlement methods', 'inventory', 'medium', FALSE, 'active')
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
        'inventory.supplier.qualification.manage',
        'inventory.supplier.settlement.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

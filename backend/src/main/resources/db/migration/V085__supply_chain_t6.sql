-- R5 T6 供应链三模式 + 采购周期 + 结构化库房 + 域内高价值复核（供应链×库存域）。
-- 裁决依据：裁决④（三模式合同 + 采购周期完整流转 + 结构化库房 三项 P0）；二轮裁决（高价值复核=库存域内自建）。
-- 依据：docs/r5-spec-T6-supply-chain-dev-design-20260618.md §1；现状取自 V012/V027/V046。
-- 口径：金额=整数分（D1）、CHECK 中英枚举（D7）、隔离列 project_id/merchant_id。
-- 本迁移一次性建全部 T6 表（四块），供后续代理接采购周期/高价值复核服务（不再加迁移）。
--   1) supply_contracts            —— 本代理已接 SupplyContractService（建/审批/可采购范围卡控）。
--   2) warehouses + balances.warehouse_id —— 本代理已接 WarehouseService（库房 CRUD + 库存挂 warehouse_id）。
--   3) inventory_purchase_requests 状态机扩展 + inventory_purchase_request_events —— 留代理2（采购周期）。
--   4) inventory_high_value_reviews —— 留代理3（高价值复核）。

-- ── 1) 供应合同三模式 ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS supply_contracts (
    id VARCHAR(64) PRIMARY KEY,
    contract_no VARCHAR(64) NOT NULL UNIQUE,
    supplier_id VARCHAR(64) NOT NULL REFERENCES inventory_suppliers(id),
    project_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),                          -- 商户自采时归属商户
    contract_mode VARCHAR(40) NOT NULL,               -- 三模式
    supply_scope TEXT,                                -- 供货范围
    price_terms TEXT,                                 -- 价格口径摘要
    payment_term_days INTEGER,                        -- 账期
    valid_from DATE,
    valid_to DATE,
    summary TEXT,                                     -- 合同核心摘要
    purchasable_scope JSONB NOT NULL DEFAULT '[]'::jsonb,  -- 可采购物料/范围（卡控用）
    approval_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_supply_contracts_mode CHECK (contract_mode IN
      ('platform_self_use', 'merchant_self_purchase', 'platform_purchase_distribution')),
    CONSTRAINT chk_supply_contracts_approval CHECK (approval_status IN ('draft', 'approved', 'rejected')),
    CONSTRAINT chk_supply_contracts_status CHECK (status IN ('active', 'suspended', 'expired'))
);

CREATE INDEX IF NOT EXISTS idx_supply_contracts_supplier
    ON supply_contracts (supplier_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_supply_contracts_scope
    ON supply_contracts (project_id, merchant_id, approval_status);

-- ── 2) 结构化库房（库位仍用 location_id，挂 warehouse） ───────────────────────
CREATE TABLE IF NOT EXISTS warehouses (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),
    warehouse_type VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_warehouses_type CHECK (warehouse_type IN
      ('main', 'bar', 'bar_counter', 'consumable', 'gift', 'saved_bottle')),
    CONSTRAINT chk_warehouses_status CHECK (status IN ('active', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_warehouses_scope
    ON warehouses (project_id, merchant_id, status);

ALTER TABLE inventory_stock_balances
    ADD COLUMN IF NOT EXISTS warehouse_id VARCHAR(64) REFERENCES warehouses(id);

-- ── 3) 采购周期：扩 inventory_purchase_requests 状态机 + 流转字段 + 留痕事件表 ──
--    （表与列本迁移建好，状态机服务留代理2）。
ALTER TABLE inventory_purchase_requests
    ADD COLUMN IF NOT EXISTS supply_contract_id VARCHAR(64) REFERENCES supply_contracts(id),
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS ordered_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS actual_arrival_date DATE,
    ADD COLUMN IF NOT EXISTS received_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS received_by VARCHAR(64);

-- 状态枚举收口（原自由 status → CHECK 全流程）。
-- 现状核对：InventoryService.createPurchaseRequest 写入 status='created'（非 7 态全流程枚举），
-- 历史数据/既有写入皆为 'created'，因此 CHECK 取值集须含 'created'（兼容现状），否则迁移会失败。
-- 幂等：仅当约束不存在时添加。
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_inv_purchase_status'
    ) THEN
        ALTER TABLE inventory_purchase_requests
            ADD CONSTRAINT chk_inv_purchase_status CHECK (status IN
              ('created', 'submitted', 'approved', 'rejected', 'ordered', 'expected', 'arrived', 'received'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS inventory_purchase_request_events (
    id VARCHAR(64) PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL REFERENCES inventory_purchase_requests(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    operator_user_id VARCHAR(64) NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_inventory_purchase_request_events_request
    ON inventory_purchase_request_events (request_id, created_at DESC);

-- ── 4) 库存域内高价值复核（裁决：域内自建，不走 governance） ───────────────────
--    （表本迁移建好，复核服务留代理3）。
CREATE TABLE IF NOT EXISTS inventory_high_value_reviews (
    id VARCHAR(64) PRIMARY KEY,
    subject_type VARCHAR(40) NOT NULL,                -- 报损/盘点差异/存酒作废
    subject_id VARCHAR(64) NOT NULL,                  -- 关联 movement/adjustment/saved_bottle
    photo_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    video_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    reviewer_user_id VARCHAR(64),
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_inv_hv_subject CHECK (subject_type IN ('damage_report', 'stock_variance', 'saved_bottle_void')),
    CONSTRAINT chk_inv_hv_status CHECK (review_status IN ('pending', 'approved', 'rejected'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_high_value_reviews_status
    ON inventory_high_value_reviews (review_status, created_at DESC);

-- ── 5) 权限点 seed（T6 §5） ─────────────────────────────────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('inventory.supply_contract.manage', 'Manage supply contracts (three modes)', 'inventory', 'high', TRUE, 'active'),
    ('inventory.purchase.approve',       'Approve inventory purchase requests',   'inventory', 'high', TRUE, 'active'),
    ('inventory.warehouse.manage',       'Manage structured warehouses',          'inventory', 'high', TRUE, 'active'),
    ('inventory.high_value.review',      'Review inventory high-value subjects',   'inventory', 'high', TRUE, 'active')
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
        'inventory.supply_contract.manage',
        'inventory.purchase.approve',
        'inventory.warehouse.manage',
        'inventory.high_value.review'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

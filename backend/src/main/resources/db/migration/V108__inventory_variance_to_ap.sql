-- P1#13 库存差异 → AR/AP 入账（finance-ar-ap §6D / R6-D08）。
-- 现状证据：无 stocktake / inventory_variance 表；FinanceService.SOURCE_TYPES 无 inventory_difference。
-- 本迁移收口（库存盘点差异经复核入账成 ar_ap_items，科目方向按 R6-D08）：
--   1) stocktake：盘点单（盘点单据头，含范围/状态）。
--   2) inventory_variance：库存差异明细（item/batch/数量差/金额差/原因/复核状态），
--      复核通过后由 finance 服务生成 ar_ap_items(source_type='inventory_difference')。
--   3) 给 ar_ap_items 允许 source_type='inventory_difference'（既有 source_type 为自由 VARCHAR，无 CHECK，
--      服务层 SOURCE_TYPES 增加枚举即可，DB 层无需改约束）。
-- 跨域只读：stocktake/inventory_variance 由供应链域盘点流程写入，finance 仅只读差异并入账，不改其写路径。

-- ── 1) 盘点单 ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS stocktake (
    id VARCHAR(64) PRIMARY KEY,
    stocktake_no VARCHAR(64) NOT NULL UNIQUE,
    warehouse_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),                            -- 商户维度（隔离用，平台库存为空）
    business_date DATE NOT NULL,                        -- 盘点经营日
    status VARCHAR(32) NOT NULL DEFAULT 'counting',     -- counting|reviewing|closed
    remark VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_stocktake_status CHECK (status IN ('counting', 'reviewing', 'closed'))
);

CREATE INDEX IF NOT EXISTS idx_stocktake_warehouse_date
    ON stocktake (warehouse_id, business_date);

CREATE INDEX IF NOT EXISTS idx_stocktake_merchant
    ON stocktake (merchant_id)
 WHERE merchant_id IS NOT NULL;

-- ── 2) 库存差异明细 ──────────────────────────────────────────────────────────
-- variance_qty = 实盘 − 账面（负=盘亏/报损，正=盘盈）；variance_amount_cent 同号。
-- reason：报损 damage / 盘亏 shortage / 盘盈 overage / 供应商责任 supplier_fault。
-- review_status：pending（待复核）|approved（复核通过，可入账）|rejected（驳回，不入账）。
-- posted_ar_ap_item_id：复核通过入账后回填生成的 ar_ap_items.id（幂等防重复入账）。
CREATE TABLE IF NOT EXISTS inventory_variance (
    id VARCHAR(64) PRIMARY KEY,
    stocktake_id VARCHAR(64) NOT NULL REFERENCES stocktake(id) ON DELETE CASCADE,
    item_id VARCHAR(64) NOT NULL,                       -- 物料/商品 id（供应链域，只读引用）
    batch_no VARCHAR(64),                               -- 批次（可空）
    book_qty NUMERIC(18, 3) NOT NULL DEFAULT 0,         -- 账面数量
    counted_qty NUMERIC(18, 3) NOT NULL DEFAULT 0,      -- 实盘数量
    variance_qty NUMERIC(18, 3) NOT NULL,              -- 数量差（带符号）
    variance_amount_cent BIGINT NOT NULL,              -- 金额差（带符号，分）
    reason VARCHAR(32) NOT NULL,                        -- 报损/盘亏/盘盈/供应商责任
    supplier_id VARCHAR(64),                            -- 供应商责任时的责任供应商（→ AP 对手方）
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    reviewed_by VARCHAR(64),
    reviewed_at TIMESTAMPTZ,
    posted_ar_ap_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_inventory_variance_reason
        CHECK (reason IN ('damage', 'shortage', 'overage', 'supplier_fault')),
    CONSTRAINT chk_inventory_variance_review
        CHECK (review_status IN ('pending', 'approved', 'rejected'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_variance_stocktake
    ON inventory_variance (stocktake_id);

CREATE INDEX IF NOT EXISTS idx_inventory_variance_review_status
    ON inventory_variance (review_status);

-- 幂等：同一差异行只能入账一次（posted_ar_ap_item_id 非空时唯一）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_variance_posted
    ON inventory_variance (id)
 WHERE posted_ar_ap_item_id IS NOT NULL;

-- ── 3) seed 权限（参照 V008/V069 finance 权限 INSERT 模式）────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.inventory_variance.post', 'Post reviewed inventory variance to AR/AP', 'finance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('finance.inventory_variance.post')
ON CONFLICT (role_id, permission_code) DO NOTHING;

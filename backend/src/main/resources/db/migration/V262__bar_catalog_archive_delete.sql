-- V262 数据存档与审批后删除范式铺到酒吧配置目录三实体（数据删除 Tier-A #3，照 V255 权益模板 / V259 商户 / V260 票种范式）。
-- 用户拍板(2026-07-06)："所有数据都应有存档或删除的能力,可要求审批,不能录入后就不可变。"
-- 本迁移覆盖 bar_products（酒吧商品）/ bar_product_categories（商品分类）/ bar_combos（套餐）。
--
-- 存档轴：archived_at 非空=已存档；列表默认 WHERE archived_at IS NULL 过滤（软下线可恢复，照 V255 先例）。
-- 删除轴：delete_status none->pending（申请）->approved（执行前瞬时态，随即物理 DELETE）；
--   申请须先存档、审批禁自审、无活引用方可删。
-- 全列可空/带默认，向后兼容，可独立回滚。
-- 设计详见 docs/data-archive-delete-pattern-design-v0.1.md。

-- ============ bar_products（酒吧商品） ============
ALTER TABLE bar_products
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_bp_delete_status') THEN
        ALTER TABLE bar_products
            ADD CONSTRAINT chk_bp_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bar_products_active_list
    ON bar_products (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- ============ bar_product_categories（商品分类） ============
ALTER TABLE bar_product_categories
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_bpc_delete_status') THEN
        ALTER TABLE bar_product_categories
            ADD CONSTRAINT chk_bpc_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bar_product_categories_active_list
    ON bar_product_categories (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- ============ bar_combos（套餐） ============
ALTER TABLE bar_combos
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_bc_delete_status') THEN
        ALTER TABLE bar_combos
            ADD CONSTRAINT chk_bc_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bar_combos_active_list
    ON bar_combos (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- ============ 审批权限码（3 个，domain=bar，critical，requires_approval）============
-- 与既有 bar.*.manage 职责分离：发起删除权≠审批删除权（照 V255 supervisor_review 原则）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.product.approve', '审批酒吧商品删除', 'bar', 'critical', TRUE, 'active'),
    ('bar.category.approve', '审批酒吧商品分类删除', 'bar', 'critical', TRUE, 'active'),
    ('bar.combo.approve', '审批酒吧套餐删除', 'bar', 'critical', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

-- 注意：V261 已把「当时」全部 active 权限授 role_platform_admin，但本迁移新插的 3 个权限晚于 V261，
-- 不在其覆盖范围内，须按 V261 确立的基线自行补授（新权限迁移必须同步授 role_platform_admin）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'bar.product.approve',
        'bar.category.approve',
        'bar.combo.approve'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

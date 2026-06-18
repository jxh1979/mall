-- R3 修复 P0-6/P0-7：积分规则引擎 + 礼物表收敛（REQ-12/14）。
-- 背景：R3 确认字面 points_rules/point_gifts 表不存在；积分账户/明细/兑换/目录已以 customer_ 前缀存在。
-- 收敛口径（backlog 推荐①）：新建 customer_points_rules 承接规则引擎；customer_redemption_catalog_items 正式承接
-- "礼物(point_gifts)"语义（已有 stock_quantity + 关联库存物料），补 value_amount_cent/project_id 即可，不另建 point_gifts 表。
-- 兑换扣库存机制已在代码层从 "loss" 调整改为 redeem_consume typed-movement（见 V0 代码 + InventoryStockMovementService 白名单）。
-- 隔离键用 project_id（综合体级会员中心，非 merchant）；单综合体默认全局可见，多综合体启用时收紧。

CREATE TABLE IF NOT EXISTS customer_points_rules (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    rule_name VARCHAR(160) NOT NULL,
    earn_method VARCHAR(40) NOT NULL,
    earn_ratio BIGINT NOT NULL DEFAULT 0,
    level_threshold JSONB NOT NULL DEFAULT '{}',
    validity_type VARCHAR(32) NOT NULL,
    validity_days INTEGER,
    expire_mode VARCHAR(40) NOT NULL,
    clear_day VARCHAR(8),
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cpr_earn_method CHECK (earn_method IN ('consume_amount','check_in','activity')),
    CONSTRAINT chk_cpr_validity_type CHECK (validity_type IN ('permanent','fixed_days','natural_year')),
    CONSTRAINT chk_cpr_expire_mode CHECK (expire_mode IN ('fifo_batch','whole_account_yearly')),
    CONSTRAINT chk_cpr_status CHECK (status IN ('draft','active','paused','expired')),
    CONSTRAINT chk_cpr_earn_ratio CHECK (earn_ratio >= 0),
    CONSTRAINT chk_cpr_validity_days CHECK (validity_days IS NULL OR validity_days >= 0)
);
CREATE INDEX IF NOT EXISTS idx_customer_points_rules_project ON customer_points_rules(project_id) WHERE project_id IS NOT NULL;

-- 礼物收敛：customer_redemption_catalog_items 正式承接 point_gifts 语义
ALTER TABLE customer_redemption_catalog_items
    ADD COLUMN IF NOT EXISTS value_amount_cent BIGINT,
    ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_crci_value_amount_nonneg') THEN
        ALTER TABLE customer_redemption_catalog_items ADD CONSTRAINT chk_crci_value_amount_nonneg
            CHECK (value_amount_cent IS NULL OR value_amount_cent >= 0);
    END IF;
END $$;

-- V296 库存进货/出库成本台账（移动加权平均）——持股分红净利润制的前置数据基建（2026-07-18 拍板）。
-- 口径：
-- 1) inventory_stock_movements 增列 unit_cost_cent / total_cost_cent（整数分，出入库流水都记成本）。
--    NULL = 无成本基准：存量库存未录进货价时不编造数字、不阻断出库；首次录入进货价后开始滚动。
-- 2) 新表 inventory_item_cost_state：商户+物料粒度的移动加权成本状态行（当前均价/持有数量/累计出入成本）。
--    物料唯一归属商户（inventory_items.merchant_id），故 item_id 主键即商户+物料粒度；
--    merchant_id 为冗余归属列，供期间成本汇总按商户过滤。
--    并发出入库对同一物料的均价更新经 SELECT FOR UPDATE 行锁串行化（写路径均在 @Transactional 内）。
-- 3) cost_method 为计价方法配置字段（凡涉数据皆可配），本期仅实现 moving_average（移动加权）一种，
--    CHECK 约束仅放行该取值，后续扩展算法时随迁移放开。
-- 4) 出库成本 = 出库时点按状态行总成本比例扣转（数量清零时总成本精确清零，避免舍入漂移）；
--    调拨（源/目标库位同时存在）只在流水上记参考成本，不改状态行（物料仍在同一商户持有）。

ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS unit_cost_cent BIGINT;
ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS total_cost_cent BIGINT;

ALTER TABLE inventory_stock_movements
    ADD CONSTRAINT chk_inventory_stock_movements_unit_cost
        CHECK (unit_cost_cent IS NULL OR unit_cost_cent >= 0);
ALTER TABLE inventory_stock_movements
    ADD CONSTRAINT chk_inventory_stock_movements_total_cost
        CHECK (total_cost_cent IS NULL OR total_cost_cent >= 0);

CREATE TABLE IF NOT EXISTS inventory_item_cost_state (
    item_id VARCHAR(64) PRIMARY KEY REFERENCES inventory_items(id),
    merchant_id VARCHAR(64),
    on_hand_quantity BIGINT NOT NULL DEFAULT 0,
    total_cost_cent BIGINT NOT NULL DEFAULT 0,
    avg_unit_cost_cent BIGINT NOT NULL DEFAULT 0,
    cumulative_in_cost_cent BIGINT NOT NULL DEFAULT 0,
    cumulative_out_cost_cent BIGINT NOT NULL DEFAULT 0,
    cost_method VARCHAR(32) NOT NULL DEFAULT 'moving_average',
    has_cost_basis BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_inventory_item_cost_state_qty CHECK (on_hand_quantity >= 0),
    CONSTRAINT chk_inventory_item_cost_state_total CHECK (total_cost_cent >= 0),
    CONSTRAINT chk_inventory_item_cost_state_avg CHECK (avg_unit_cost_cent >= 0),
    CONSTRAINT chk_inventory_item_cost_state_cum_in CHECK (cumulative_in_cost_cent >= 0),
    CONSTRAINT chk_inventory_item_cost_state_cum_out CHECK (cumulative_out_cost_cent >= 0),
    CONSTRAINT chk_inventory_item_cost_state_method CHECK (cost_method IN ('moving_average'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_item_cost_state_merchant
    ON inventory_item_cost_state (merchant_id);

-- 期间出库成本汇总（分红引擎成本侧取数点）：出库行 = 仅源库位（source 非空且 target 空）。
CREATE INDEX IF NOT EXISTS idx_inventory_stock_movements_cost_summary
    ON inventory_stock_movements (merchant_id, created_at)
 WHERE source_location_id IS NOT NULL AND target_location_id IS NULL;

-- 5) 成本=内部口径，沿用 2026-07-08 内部结算价拍板范式（V279）：独立高危权限，仅授权后台可见，
--    店员 POS/终端不可见。无此权限的调用方：流水响应成本列脱敏置 NULL，成本两端点直接 403。
--    平台管理员直授（V206 范式），其它角色按需另行授权。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('inventory.cost.view', 'View inventory cost ledger (moving average cost)', 'inventory', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'inventory.cost.view'
ON CONFLICT (role_id, permission_code) DO NOTHING;

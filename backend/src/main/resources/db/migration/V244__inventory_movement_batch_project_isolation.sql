-- 库存域 C2 读侧半开补齐（审计遗留）。
-- 基线 origin/main=95c0535；当前最高迁移 V243，本迁移取 V244。
--
-- V237 只做了 inventory_stock_balances 的项目级读隔离（merchant→project→全量）。审计指出流水列表
-- 读通道仍缺 project 维隔离：项目 A 员工经 /stock-movements、/stock-batches 列表仍可越读项目 B / 全平台
-- 流水。本迁移给这两张表补 project_id 列（写入时由服务层从 currentProjectScopeQuietly 落库），并回填历史行。
--
-- inventory_stock_adjustments 无任何列表读通道（仅 create + 按 id 复核），不构成跨租户列表泄漏面，故不涉及。
-- project_id 保持 NULL=不限项目（与 V237 余额侧、既有 merchant_id NULL 放行语义一致，避免历史行回归）。

-- ── 流水行项目归属列 ──────────────────────────────────────────────────────────
ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_movements_project
    ON inventory_stock_movements (project_id)
 WHERE project_id IS NOT NULL;

-- ── 批次行项目归属列 ──────────────────────────────────────────────────────────
ALTER TABLE inventory_stock_batches ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_batches_project
    ON inventory_stock_batches (project_id)
 WHERE project_id IS NOT NULL;

-- ── 回填历史行 ────────────────────────────────────────────────────────────────
-- 从关联余额行推导 project_id：同 item+location 的库存余额已在 V237 起携带 project_id。
-- 推不出（无匹配余额行）的历史行留 NULL（=不限项目，走全量读通道，不产生回归）。
UPDATE inventory_stock_movements m
   SET project_id = b.project_id
  FROM inventory_stock_balances b
 WHERE m.project_id IS NULL
   AND b.project_id IS NOT NULL
   AND b.item_id = m.item_id
   AND b.location_id = COALESCE(m.target_location_id, m.source_location_id);

UPDATE inventory_stock_batches t
   SET project_id = b.project_id
  FROM inventory_stock_balances b
 WHERE t.project_id IS NULL
   AND b.project_id IS NOT NULL
   AND b.item_id = t.item_id
   AND b.location_id = t.location_id;

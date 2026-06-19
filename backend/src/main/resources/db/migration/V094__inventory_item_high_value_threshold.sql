-- R5 T6-4：报损(damage_report)/盘点差异(stock_variance)高价值入口接入域内复核。
-- 背景：T6-3 已建 inventory_high_value_reviews + InventoryHighValueReviewService（saved_bottle_void 闭环），
--   damage_report/stock_variance 子类型表已支持但报损/盘点差异入口暂未接复核（V085 §4 遗留）。本迁移补判定阈值字段。
--
-- 高价值判定口径：调整金额（数量 × 单价）≥ 物料阈值 high_value_threshold_cents。
--   - inventory_items.high_value_threshold_cents 可空：null = 该物料不视为高价值/不触发复核（向后兼容，既有物料默认 null）。
--   - 单价来自调整请求的 unit_cost_cents（库存域无存储单价）；为留痕与 approve 时复算，inventory_stock_adjustments
--     增 unit_cost_cents（可空）。两者任一为空 → 不视高价值 → 维持即时调整（向后兼容）。
-- 口径与 saved_bottles.high_value_threshold_cents(V061)/D1（整数分）一致。

ALTER TABLE inventory_items
    ADD COLUMN IF NOT EXISTS high_value_threshold_cents BIGINT;

ALTER TABLE inventory_items
    DROP CONSTRAINT IF EXISTS chk_inventory_items_high_value_threshold;
ALTER TABLE inventory_items
    ADD CONSTRAINT chk_inventory_items_high_value_threshold
    CHECK (high_value_threshold_cents IS NULL OR high_value_threshold_cents >= 0);

-- 调整单留痕单价（高价值复算用）；可空兼容既有写入。
ALTER TABLE inventory_stock_adjustments
    ADD COLUMN IF NOT EXISTS unit_cost_cents BIGINT;

ALTER TABLE inventory_stock_adjustments
    DROP CONSTRAINT IF EXISTS chk_inventory_stock_adjustments_unit_cost;
ALTER TABLE inventory_stock_adjustments
    ADD CONSTRAINT chk_inventory_stock_adjustments_unit_cost
    CHECK (unit_cost_cents IS NULL OR unit_cost_cents >= 0);

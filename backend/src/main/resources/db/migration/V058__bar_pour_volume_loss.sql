-- R3 修复 P0-3：按杯出酒量/损耗（REQ-22）。
-- 背景：R3 确认 standard_pour_volume/actual_pour_volume/loss_threshold 等迁移零命中，矩阵"已覆盖"为设计稿声明。
-- bar_products：每份标准出酒量 + 单位 + 损耗阈值（按杯销售配置）。
-- bar_order_items：实际出酒量 + 超标损耗量。出品(deliver)时按标准量扣减，超出记 loss_volume，超阈值触发 inventory.loss_exception 留痕核查。
-- 容量单位为整数（如 ml），与金额整数分一致的离散化口径。

ALTER TABLE bar_products
    ADD COLUMN IF NOT EXISTS standard_pour_volume INTEGER,
    ADD COLUMN IF NOT EXISTS pour_volume_unit VARCHAR(16),
    ADD COLUMN IF NOT EXISTS loss_threshold INTEGER;

ALTER TABLE bar_order_items
    ADD COLUMN IF NOT EXISTS actual_pour_volume INTEGER,
    ADD COLUMN IF NOT EXISTS loss_volume INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_bar_products_pour_nonneg') THEN
        ALTER TABLE bar_products ADD CONSTRAINT chk_bar_products_pour_nonneg
            CHECK ((standard_pour_volume IS NULL OR standard_pour_volume >= 0)
               AND (loss_threshold IS NULL OR loss_threshold >= 0));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_bar_order_items_pour_nonneg') THEN
        ALTER TABLE bar_order_items ADD CONSTRAINT chk_bar_order_items_pour_nonneg
            CHECK ((actual_pour_volume IS NULL OR actual_pour_volume >= 0) AND loss_volume >= 0);
    END IF;
END $$;

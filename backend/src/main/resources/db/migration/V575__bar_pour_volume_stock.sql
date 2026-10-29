-- 文件版本：V0.01
-- V575：第十八批 I 段第 76 条 76-R1 按杯按标准出酒量（按容量）扣库存的主数据与流水列。
-- 依据：BSM 第 1065 行 / ENUM 第 106 行「按杯按标准出酒量扣库存、超出记损耗……未配备采集设备的吧台，必须按标准出酒量配酒，
--   也按该标准计算扣减与标准损耗，不得按零损耗记账」；BSM 第 1168 行「按杯消耗按标准容量扣减，超出部分进入损耗。」；
--   ENUM 第 79 行「定价、库存、结算、对账一律按商品规格逐项进行」（例「某某威士忌 700 毫升瓶装」）。
-- 库存仍按商品规格记瓶数；已开瓶余量按毫升记在库存余额行，开新瓶才减瓶数。按杯出品的出酒量与损耗量记在同一条流水上，
-- 只动已开瓶余量、没开新瓶的流水瓶数为 0。铁律 8：不为存量数据回填；只加列带默认。

-- 1) 物料规格容量（毫升），如 700 毫升瓶装填 700；不按容量扣的物料留空。
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS spec_volume_ml INTEGER;
ALTER TABLE inventory_items DROP CONSTRAINT IF EXISTS chk_inventory_items_spec_volume_ml;
ALTER TABLE inventory_items ADD CONSTRAINT chk_inventory_items_spec_volume_ml
    CHECK (spec_volume_ml IS NULL OR spec_volume_ml > 0);
COMMENT ON COLUMN inventory_items.spec_volume_ml IS '规格容量（毫升）：按杯按标准出酒量扣库存的物料须登记（如 700 毫升瓶装填 700）';

-- 2) 库存行已开瓶余量（毫升）：按杯出品先用余量，不够再开新瓶（瓶数减 1、余量加一瓶容量）。
ALTER TABLE inventory_stock_balances ADD COLUMN IF NOT EXISTS opened_volume_ml INTEGER NOT NULL DEFAULT 0;
ALTER TABLE inventory_stock_balances DROP CONSTRAINT IF EXISTS chk_inventory_stock_balances_opened_volume_ml;
ALTER TABLE inventory_stock_balances ADD CONSTRAINT chk_inventory_stock_balances_opened_volume_ml
    CHECK (opened_volume_ml >= 0);
COMMENT ON COLUMN inventory_stock_balances.opened_volume_ml IS '已开瓶余量（毫升）：已开瓶的酒不再计入瓶数，余量记在这里';

-- 3) 流水出酒量与损耗量（毫升）：按杯出品、退品回库与驳回重扣时填；按瓶为空。只动余量的流水瓶数为 0、须带出酒量。
ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS volume_ml INTEGER;
ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS loss_volume_ml INTEGER;
ALTER TABLE inventory_stock_movements DROP CONSTRAINT IF EXISTS chk_inventory_stock_movements_volume_ml;
ALTER TABLE inventory_stock_movements ADD CONSTRAINT chk_inventory_stock_movements_volume_ml
    CHECK ((volume_ml IS NULL OR volume_ml > 0) AND (loss_volume_ml IS NULL OR loss_volume_ml >= 0));
ALTER TABLE inventory_stock_movements DROP CONSTRAINT IF EXISTS chk_inventory_stock_movements_quantity;
ALTER TABLE inventory_stock_movements ADD CONSTRAINT chk_inventory_stock_movements_quantity
    CHECK (quantity > 0 OR (quantity = 0 AND volume_ml IS NOT NULL));
COMMENT ON COLUMN inventory_stock_movements.volume_ml IS '出酒量（毫升）：按杯按标准出酒量扣减或退回的量；按瓶为空';
COMMENT ON COLUMN inventory_stock_movements.loss_volume_ml IS '损耗量（毫升）：按杯出品连同出酒量一并扣减的损耗（有设备＝实采超出部分，无设备＝标准损耗）';

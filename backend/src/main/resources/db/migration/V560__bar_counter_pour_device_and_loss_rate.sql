-- 文件版本：V0.01
-- V560：第十八批 C 段第 17 条 吧台按有无出酒量采集设备二分；无设备按标准量配酒并记标准损耗；酒吧损耗率阈值。
-- 依据：BSM 第 1065 行 / ENUM 第 106 行「配有出酒量采集设备的吧台，实际出酒量必须由设备采集；未配备采集设备的吧台，必须按标准出酒量配酒，
--   也按该标准计算扣减与标准损耗，不得按零损耗记账」；BSM 第 2015 行「损耗率（当期损耗量 ÷ 标准出酒量）超过后台配置的阈值即转核查工单
--   （安全阈值，按酒吧可配，出厂默认 5%，须同时配置上下限）」；第十八批裁决 4「标准损耗＝标准出酒量×本酒吧损耗率阈值（……出厂 5%，这个阈值一并做出来）」。
-- 铁律 8：不为存量数据回填；只加列带默认。

-- 1) 酒吧损耗率阈值：按酒吧可配（酒吧＝merchant_type='bar' 的商户，V267），值与上下限同配（范式同 V521），整数百分比；
--    未配置＝三列皆空，按出厂 5%。下限不得为 0：标准损耗＝标准出酒量×该阈值，阈值为 0 即零损耗（BSM 第 1065 行「不得按零损耗记账」）。
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS pour_loss_rate_pct INTEGER;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS pour_loss_rate_min_pct INTEGER;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS pour_loss_rate_max_pct INTEGER;
ALTER TABLE merchants DROP CONSTRAINT IF EXISTS chk_merchants_pour_loss_rate;
ALTER TABLE merchants ADD CONSTRAINT chk_merchants_pour_loss_rate CHECK (
    (pour_loss_rate_pct IS NULL AND pour_loss_rate_min_pct IS NULL AND pour_loss_rate_max_pct IS NULL)
    OR (pour_loss_rate_pct IS NOT NULL AND pour_loss_rate_min_pct IS NOT NULL AND pour_loss_rate_max_pct IS NOT NULL
        AND pour_loss_rate_min_pct >= 1
        AND pour_loss_rate_min_pct <= pour_loss_rate_pct
        AND pour_loss_rate_pct <= pour_loss_rate_max_pct
        AND pour_loss_rate_max_pct <= 100)
);
COMMENT ON COLUMN merchants.pour_loss_rate_pct IS '酒吧损耗率阈值（%）：未配采集设备的吧台按杯出品记标准损耗＝标准出酒量×该阈值（未配置＝空，按出厂 5；与上下限同配）';
COMMENT ON COLUMN merchants.pour_loss_rate_min_pct IS '酒吧损耗率阈值已配置下限（%，与值、上限同配，不小于 1）';
COMMENT ON COLUMN merchants.pour_loss_rate_max_pct IS '酒吧损耗率阈值已配置上限（%，与值、下限同配）';

-- 2) 吧台（库房分类「吧台库存」warehouse_type='bar_counter'，ENUM 第 160 行）是否配有出酒量采集设备；物理事实，默认未配。
ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS pour_device_equipped BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE warehouses DROP CONSTRAINT IF EXISTS chk_warehouses_pour_device_bar_counter;
ALTER TABLE warehouses ADD CONSTRAINT chk_warehouses_pour_device_bar_counter
    CHECK (pour_device_equipped = FALSE OR warehouse_type = 'bar_counter');
COMMENT ON COLUMN warehouses.pour_device_equipped IS '吧台是否配有出酒量采集设备（只对吧台库存可置 TRUE）：配有的按杯出品必须有设备采集的实际出酒量，未配的按标准出酒量配酒并记标准损耗';

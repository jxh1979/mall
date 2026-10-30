-- 文件版本：V0.01
-- V580：第十八批 K 段第 97 条 97-R3 酒吧订单明细记套餐来源。
-- 依据：BSM 第 1168 行「记录吧台售卖后的酒水消耗，支持按杯、按瓶、按套餐或其他形式统计」；BSM 第 1065 行「按杯、按瓶、套餐」。
-- 套餐下单展开成单品明细（BarOrderService.createOrderInternal），展开出的明细记所属套餐，吧台消耗统计据此分出「套餐」。
-- 单点的明细为空。套餐删除只改 delete_status、不物理删，外键不会被删除挡住。铁律 8：不为存量数据回填。

ALTER TABLE bar_order_items ADD COLUMN IF NOT EXISTS combo_id VARCHAR(64) REFERENCES bar_combos(id);
COMMENT ON COLUMN bar_order_items.combo_id IS '套餐来源：套餐下单展开出的明细记所属套餐；单点为空';

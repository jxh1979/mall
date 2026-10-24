-- 文件版本：V0.01
-- 第十三批 B 段 ROOT-03-R27（BSM 第 756 行、ENUM 第 58 行「多种资格价不叠加，选择最终价格最低的一项。订单保存基础价、动态价、全部命中资格价、
-- 最终选中价和规则版本」）：客人自助购票由系统判定资格，订单另存全部命中资格价（每条含规则编号、类型、单价与规则更新时刻作为版本快照）；
-- 基础价、动态价沿用 base_amount_cent / dynamic_adjustment_cent / dynamic_pricing_rule_id，最终选中规则沿用 price_rule_id。
-- 员工与一体机代客下单不判资格，此列为空。
ALTER TABLE ticket_orders ADD COLUMN price_hits JSONB;

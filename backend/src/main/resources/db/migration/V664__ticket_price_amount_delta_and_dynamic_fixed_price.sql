-- 文件版本：V0.01
-- V664（第十八批 AW 段第 338 条 338-R1、338-R2）：派生价加「按基础价加减金额」口径、动态价加「固定价」口径。
-- 依据：BSM:782「会员价、渠道价、动态价都在对应基础价上按打折/减免/加价/固定额派生；动态价按时段、容量等规则配置，可先试算再发布」。
-- ① 派生价 price_mode 新增 amount_delta（无 CHECK，取值由后端校验）：加减金额带符号，负数为减免、正数为加价，按对应日的基础价加减、最低 0 元。
-- ② 动态价 adjustment_type 新增 fixed_price：命中即公开价为 adjustment_amount_cent 这个固定价（仍受最低价、最高价夹值），与派生价 fixed_price 同口径。
ALTER TABLE ticket_price_rules ADD COLUMN adjustment_amount_cent BIGINT;
COMMENT ON COLUMN ticket_price_rules.adjustment_amount_cent IS
    '按基础价加减金额（分，带符号：负数为减免、正数为加价），只在 price_mode=amount_delta 时有值';

ALTER TABLE ticket_dynamic_pricing_rules DROP CONSTRAINT chk_ticket_dynamic_pricing_adjustment_type;
ALTER TABLE ticket_dynamic_pricing_rules ADD CONSTRAINT chk_ticket_dynamic_pricing_adjustment_type
    CHECK (adjustment_type IN ('percent_delta', 'fixed_delta', 'fixed_price'));

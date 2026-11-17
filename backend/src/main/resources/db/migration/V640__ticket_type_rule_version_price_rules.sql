-- 文件版本：V0.01
-- V640（第十八批 AS 段第 297 条 297-R1，第十八批裁决 63）：跨域组合通票票种规则版本记下派生价与动态价两份清单。
-- 依据：第十八批裁决 63「跨域组合通票的派生价、动态价规则也要经项目之上的平台权限审批，并记入票种规则版本。」；ENUM:32「跨域组合通票的定价与域间结算规则
--   由项目之上的平台权限统一审批」；BSM:782「会员价、渠道价、动态价都在对应基础价上按打折/减免/加价/固定额派生；动态价按时段、容量等规则配置，可先试算再发布。」
-- 草稿版与待审版里新建的派生价不进 ticket_price_rules、发布的动态价在 ticket_dynamic_pricing_rules 仍为草稿，获批时在审批同一事务里写入生效。

ALTER TABLE ticket_type_rule_versions
    ADD COLUMN price_rules JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN dynamic_pricing_rules JSONB NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN ticket_type_rule_versions.price_rules IS '派生价（会员、新人、学生、团体、渠道）：该票种全部派生价（含已存档）的人群、定价方式、三档价、折扣、渠道、资格条件与建立时间';
COMMENT ON COLUMN ticket_type_rule_versions.dynamic_pricing_rules IS '动态价：该票种已发布及本版发布的动态价规则名、优先级、生效起止、渠道、调整方式与幅度、最低最高价、条件';

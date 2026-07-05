-- V228 结算重构批3-4(PRD §6.4):专属某商户「不分摊」。
--   口径:权益标记 exclusive_merchant_id 非空时,该权益核销收入**全额归该商户**,
--         **不进 SPV 分账池、不平均分、不扣平台留存、不参与其它酒吧分池排名/客数聚合**;
--         适用「仅限某酒吧的酒水/服务」单点消费。留存只作用于进池的非专属收入。
--   本迁移:①entitlement_templates/issued_entitlements 加 exclusive_merchant_id(NULL=非专属走池;非空=专属该商户全额);
--          ②settlement_transactions 加 exclusive_merchant_id——核销双写时从权益带上,作为结算源真相的定格
--            (查询按它过滤,免得日后改模板影响历史结算);
--          发放时模板→实例复制(照批3-1/3-2 字段复制范式)。回填保持 NULL(既有权益皆非专属)。
--   仅加列,不改现有逻辑,可独立回滚。字段允许 NULL(向后兼容,NULL=非专属现状分池)。

-- ① 权益模板 + 实例:exclusive_merchant_id(专属商户,可空)
ALTER TABLE entitlement_templates
    ADD COLUMN IF NOT EXISTS exclusive_merchant_id VARCHAR(64);

ALTER TABLE issued_entitlements
    ADD COLUMN IF NOT EXISTS exclusive_merchant_id VARCHAR(64);

-- ② 结算流水:落定核销当时的专属归属(结算源真相,查询按此过滤而非 join 活模板)
ALTER TABLE settlement_transactions
    ADD COLUMN IF NOT EXISTS exclusive_merchant_id VARCHAR(64);

-- 专属核销按 exclusive_merchant_id 分组聚合直付;非专属(IS NULL)进池分账。加索引助月度引擎过滤。
CREATE INDEX IF NOT EXISTS idx_st_exclusive_merchant
    ON settlement_transactions (project_id, exclusive_merchant_id)
 WHERE transaction_type = 'verification_settled' AND exclusive_merchant_id IS NOT NULL;

-- ③ 结算明细唯一键放宽:同一酒吧当月可有「池分账」+「专属直付」两条并列明细(各 distribution_method 一条),
--    故唯一键从 (bill_id, merchant_id) 扩到 (bill_id, merchant_id, distribution_method)。
--    池分账每商户仍仅一条、专属直付每商户仅一条,不会退化出重复行;过账按明细 id 各挂独立应付,互不覆盖。
DROP INDEX IF EXISTS uq_si_bill_merchant;
CREATE UNIQUE INDEX IF NOT EXISTS uq_si_bill_merchant_method
    ON settlement_items (bill_id, merchant_id, distribution_method);

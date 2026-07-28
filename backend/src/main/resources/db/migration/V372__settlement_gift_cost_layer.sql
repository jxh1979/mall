-- V372 通票分账瀑布重建 S6:赠送权益预估成本(G 层)——列与锚点。
--
-- 【业务依据】(最终解释权=三份业务 HTML,逐条对齐、不自造口径)
--   business-system-map-for-operations §「通票收入分账全流程」(:739):
--     「再扣掉赠送权益预估成本(归娱乐平台公司):票里加赠的酒水、小食、餐饮、零售等,每一样都是一个独立权益…
--      每个权益的成本按提供方的内部结算价计;预扣金额按票种配置——按件计＝票面各权益数量 × 内部结算价之和,
--      按票计＝该票种赠送部分的售价整额」
--   §「综合／联合通票收入分账」(:742):「酒吧份额再依次扣掉赠送权益预估成本、门票综合服务费,剩余进入可分配池」
--     → G 是酒吧链上的整额扣项(设计 v3.3 §8.2:G 只记总额;by_ticket_amount 整额本就不可按业态拆)。
--   §「免费权益的成本承担」(:737):「①商户让利型:平台不承担成本、不与商户结算;②平台出资型:平台按内部
--     结算价与提供商户结算,计入赠送权益预估成本。每个权益必须在票种配置里标明成本承担方。」
--
-- 【本迁移做五件事】
--   ① settlement_transactions 加 G 两列(逐笔不可变事实,照 P6/V337 综合服务费范式:核销计提、冲正取原行之反);
--   ② chk_st_amount 扩到含 gift_cost_cent(核销行 >=0 / 冲正行 <=0,任一 SUM 对配对天然净 0);
--   ③ settlement_bills / settlement_bill_format_shares 加 G 列 + 收款主体快照(月结线性求和,不月末重算);
--   ④ ticket_type_entitlements 加 cost_bearer(成本承担方,HTML 明文「每个权益必须标明」——商户让利型不进 G
--      预扣、不与商户结算;平台出资型进 G、核销按内部结算价兑付);
--   ⑤ 收入流归属种子:pass_gift_cost → 娱乐平台公司(be_ent_platform,V354 主体骨架)。

-- ==========================================================================
-- ① settlement_transactions:G 逐笔事实两列
-- ==========================================================================

-- 计提时点=该票【首笔核销】时一次性计提(用户已拍板),锚点=parent_ticket_id(V367);
-- 「一张票一道」判定走既有部分索引 idx_st_parent_ticket,无需新索引。
ALTER TABLE settlement_transactions
    ADD COLUMN IF NOT EXISTS gift_cost_cent      BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS gift_cost_config_id VARCHAR(64);

COMMENT ON COLUMN settlement_transactions.gift_cost_cent IS
    'S6:本笔核销/冲正计提的赠送权益预估成本(G,归娱乐平台公司)。该票首笔核销一次性计提(核销为正),冲正取原行之反(为负);settleMonth 线性求和,绝不月末重算';
COMMENT ON COLUMN settlement_transactions.gift_cost_config_id IS
    'S6:本笔 G 解析到的 pass_gift_cost_configs 规则 id;供增量封顶按「项目×规则」查月内累计 + 冲正沿用';

-- chk_st_amount 扩(照 V331 加 discount_amount_cent 的先例):非冲正行全部 >=0;冲正行全部 <=0。
-- 既有行 gift_cost_cent 默认 0(同时满足两分支),重建不违反存量。
ALTER TABLE settlement_transactions DROP CONSTRAINT IF EXISTS chk_st_amount;
ALTER TABLE settlement_transactions ADD CONSTRAINT chk_st_amount CHECK (
    (transaction_type = 'verification_reversed'
        AND verified_amount_cent <= 0 AND unverified_amount_cent <= 0 AND allocated_revenue_cent <= 0
        AND discount_amount_cent <= 0 AND gift_cost_cent <= 0)
    OR (transaction_type <> 'verification_reversed'
        AND verified_amount_cent >= 0 AND unverified_amount_cent >= 0 AND allocated_revenue_cent >= 0
        AND discount_amount_cent >= 0 AND gift_cost_cent >= 0)
);

-- ==========================================================================
-- ② settlement_bills / settlement_bill_format_shares:月结留痕列
-- ==========================================================================

-- 带符号(R-3 先例):冲正月 G 合计可为负(当月净回冲)。V370 已整体放开 chk_sb_amount,无需再动 CHECK。
ALTER TABLE settlement_bills
    ADD COLUMN IF NOT EXISTS gift_cost_cent            BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS gift_beneficiary_entity_id VARCHAR(64);

COMMENT ON COLUMN settlement_bills.gift_cost_cent IS
    'S6:当月赠送权益预估成本合计(G,进池核销逐笔计提的线性求和);扣在酒吧份额上(HTML:742「酒吧份额再依次扣掉赠送权益预估成本、门票综合服务费」),归娱乐平台公司;冲正月可为负';
COMMENT ON COLUMN settlement_bills.gift_beneficiary_entity_id IS
    'S6:赠送权益预估成本收款主体快照(business_entities.id,解析自 revenue_stream_attribution 的 pass_gift_cost 流;月结时定格,后改配置不追溯已出单据)';

-- 业态份额行:G 整额记在酒吧行上(与「渠道费分摊尾差归酒吧支」同一兜底范式)。
-- by_ticket_amount 是票种级整额、不可按业态拆(设计 §8.2);按业态分记的是【兑付成本 A】,属 S7 台账,不在本列。
ALTER TABLE settlement_bill_format_shares
    ADD COLUMN IF NOT EXISTS gift_cost_cent BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN settlement_bill_format_shares.gift_cost_cent IS
    'S6:本业态承担的赠送权益预估成本(G)。整额记在酒吧业态行(HTML:742 瀑布顺序),其余业态恒 0;净份额 = 毛额 − 渠道费 − 综合服务费 − 本列';

-- ==========================================================================
-- ③ ticket_type_entitlements:成本承担方(HTML:737「每个权益必须在票种配置里标明成本承担方」)
-- ==========================================================================

-- 默认 platform_funded=平台出资型(= 改造前唯一行为:通票赠饮由娱乐平台公司按内部结算价兑付,HTML:962)
-- → 存量绑定零行为变更;商户让利型(merchant_concession)是显式配置动作:不进 G 预扣、核销不与商户结算。
ALTER TABLE ticket_type_entitlements
    ADD COLUMN IF NOT EXISTS cost_bearer VARCHAR(24) NOT NULL DEFAULT 'platform_funded';

ALTER TABLE ticket_type_entitlements DROP CONSTRAINT IF EXISTS chk_tte_cost_bearer;
ALTER TABLE ticket_type_entitlements ADD CONSTRAINT chk_tte_cost_bearer
    CHECK (cost_bearer IN ('platform_funded', 'merchant_concession'));

COMMENT ON COLUMN ticket_type_entitlements.cost_bearer IS
    'S6 免费/赠送权益的成本承担方(HTML:737):platform_funded=平台出资型(进 G 预扣·核销按内部结算价与提供商户结算);merchant_concession=商户让利型(平台不承担成本·不与商户结算·不进 G)。仅对票面原价为 0 的权益有业务含义';

-- ==========================================================================
-- ④ 收入流归属种子:赠送权益预估成本 → 娱乐平台公司
-- ==========================================================================

-- HTML:739「赠送权益预估成本(归娱乐平台公司)」——归属不是运营的自由选项,种全局默认行
-- (项目级仍可在结算配置中心覆盖,零硬编码);be_ent_platform 为 V354 落的主体骨架稳定 id。
INSERT INTO revenue_stream_attribution
    (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, is_draft, updated_by, updated_at)
SELECT 'rsa_pass_gift_cost_default', NULL, 'pass_gift_cost', 'be_ent_platform', 'entity',
       'active', FALSE, 'migration:V372', now()
 WHERE NOT EXISTS (SELECT 1 FROM revenue_stream_attribution
                    WHERE revenue_stream = 'pass_gift_cost'
                      AND project_id IS NULL AND status = 'active' AND is_draft = FALSE);

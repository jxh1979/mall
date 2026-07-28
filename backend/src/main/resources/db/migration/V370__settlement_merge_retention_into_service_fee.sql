-- V370 通票分账瀑布重建 S4:合并留存——门票综合服务费进主链。
--
-- 【业务依据】(最终解释权=三份业务 HTML,逐条对齐、不自造口径)
--   business-system-map-for-operations §「通票收入分账全流程」:
--     「再扣掉门票综合服务费(即娱乐平台公司留存/项目管理费,与娱乐平台公司留存为同一项、只扣一次)」
--     「扣完后的剩余全部进入可分配池——可分配池仅用于各酒吧分配,不再从可分配池中二次扣娱乐平台公司留存」
--   §「门票综合服务费」:该费「归娱乐平台公司所有」。
--
-- 【改造前的错版】系统里实打实存在两道:① 池内留存 = 池 × platform_retention_rules.retention_bps
--   (扣在池上,归「项目公司」);② 综合服务费(每笔核销计提,原先仅对专属商户)。同一项费被建成了两个东西,
--   业务人员对账时回答不了「这个月到底收了几道、各收了多少、归了谁」。
--
-- 【本迁移做四件事】
--   ① settlement_bills 列改造:留存三列 + 已废弃的赠酒成本列下线;综合服务费拆两支
--      (池链 pooled / 专属 exclusive)+ 收款主体快照列;
--   ② 放开带符号(R-3 用户 2026-07-28 拍板,照 V335 放开 combo_discount 的先例):
--      冲正月收入可以为负,负数月结算单要能落库(负额=向商户追回),静默钳零会丢账;
--   ③ 等价保值种子:把留存率现值(2000bps)平移进综合服务费全局默认行——上线瞬间
--      「池被扣掉两成」这件事金额逐分不变,只是名目从「项目留存」正名为「门票综合服务费」、
--      收款方从「项目公司」正名为「娱乐平台公司」(这是 HTML 要求的口径变更,不是回归);
--   ④ platform_retention_rules 停用(软删:留库可查不物理删)+ 综合服务费收入流归属种子。

-- ==========================================================================
-- ① settlement_bills 列改造
-- ==========================================================================

-- 综合服务费拆两支:原 comp_service_fee_cent 一直只记「专属直付线」抽的那部分,正名为 exclusive 支;
-- 新增 pooled 支 = 当月进池核销逐笔计提的综合服务费合计(月结只做线性求和,不重算)。
ALTER TABLE settlement_bills RENAME COLUMN comp_service_fee_cent TO comp_service_fee_exclusive_cent;
ALTER TABLE settlement_bills ADD COLUMN IF NOT EXISTS comp_service_fee_pooled_cent BIGINT NOT NULL DEFAULT 0;

-- 收款主体快照:两支综合服务费都归娱乐平台公司(business_entities,V354 六主体骨架之 be_ent_platform)。
-- 解析自 revenue_stream_attribution(ticket_service_fee),月结时定格,后改配置不追溯已出单据。
ALTER TABLE settlement_bills ADD COLUMN IF NOT EXISTS fee_beneficiary_entity_id VARCHAR(64);

-- 留存全链下线:池内不再二次扣任何留存;giveaway_cost_cent 自 D5(2026-07-21)起恒 0(赠酒补偿
-- 走 bar 域内部结算价应付),一并下线。开发环境数据最后全删,不为存量行做搬运/回填。
ALTER TABLE settlement_bills DROP COLUMN IF EXISTS retention_bps;
ALTER TABLE settlement_bills DROP COLUMN IF EXISTS retention_cent;
ALTER TABLE settlement_bills DROP COLUMN IF EXISTS spv_pool_cent;
ALTER TABLE settlement_bills DROP COLUMN IF EXISTS giveaway_cost_cent;

-- R-3 放开带符号:冲正月(退款/红冲多于新核销)收入、池、分账、费各列都可以为负——
-- 负数=当月净追回。旧 CHECK 全列 >= 0 会让负数月的月结直接崩、钱落不了账。
ALTER TABLE settlement_bills DROP CONSTRAINT IF EXISTS chk_sb_amount;

COMMENT ON COLUMN settlement_bills.available_pool_cent IS
    '可分配池(唯一的池):酒吧业态净份额 − 池链综合服务费。仅用于各酒吧分配,池内不再二次扣任何留存;冲正月可为负(向各酒吧追回)';
COMMENT ON COLUMN settlement_bills.comp_service_fee_pooled_cent IS
    '门票综合服务费·池链支:当月进池核销逐笔计提的综合服务费合计(= 娱乐平台公司留存/项目管理费,同一项只扣一次);归娱乐平台公司';
COMMENT ON COLUMN settlement_bills.comp_service_fee_exclusive_cent IS
    '门票综合服务费·专属支:当月专属商户直付前抽取的综合服务费合计(与池链支是同一道费的两支,不是第二种费);归娱乐平台公司';
COMMENT ON COLUMN settlement_bills.fee_beneficiary_entity_id IS
    '门票综合服务费收款主体快照(business_entities.id,解析自 revenue_stream_attribution 的 ticket_service_fee 流;月结时定格)';

-- ==========================================================================
-- ② settlement_items 放开带符号(与 ① 同一条 R-3 裁决)
-- ==========================================================================

-- 负池月各家分账为负 = 向酒吧追回;旧 CHECK >= 0 会把负数月的明细挡在库外。
ALTER TABLE settlement_items DROP CONSTRAINT IF EXISTS chk_si_amount;

COMMENT ON COLUMN settlement_items.final_amount_cent IS
    '该商户本月分账终额(整数分,带符号):正=应付商户,负=向商户追回(冲正月)。';

-- ==========================================================================
-- ③ settlement_bill_format_shares 补综合服务费列
-- ==========================================================================

-- 综合服务费是对所有门票核销一视同仁的笔级扣项(HTML「所有门票项目都要收一道」),
-- 游乐/餐饮份额同样要按自己名下核销计提的费净掉——否则那部分费会被错扣进酒吧池。
-- 各业态行单列本业态的费,财务对账时每个业态「毛额 − 折扣补回 − 渠道费 − 综合服务费 = 净份额」四步都看得见。
ALTER TABLE settlement_bill_format_shares ADD COLUMN IF NOT EXISTS comp_service_fee_cent BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN settlement_bill_format_shares.comp_service_fee_cent IS
    '本业态当月进池核销计提的门票综合服务费合计(归娱乐平台公司);净份额 = 毛额 − 渠道费 − 本列';

-- ==========================================================================
-- ④ 等价保值种子(K9 的核心)
-- ==========================================================================

-- 删掉池内 20% 留存而综合服务费仍配 0,各酒吧会从分 80% 变成分 100%——那不是「零行为变更」。
-- 把留存率现值平移进综合服务费全局默认行:数学上 (池 − 池×20%) 与 (池 − Σ逐笔20%费) 在同一舍入
-- 口径(MoneyRounding HALF_UP)下对齐,上线瞬间池链金额逐分不变。
-- 只在「全局默认行还是出厂 0 费」时平移——运营已经真配过费率的,不覆盖人家的配置。
UPDATE comprehensive_service_fee_configs
   SET fee_bps = COALESCE((SELECT retention_bps
                             FROM platform_retention_rules
                            WHERE project_id IS NULL AND status = 'active' AND is_draft = FALSE
                            LIMIT 1), 2000),
       updated_at = now()
 WHERE project_id IS NULL AND merchant_id IS NULL AND ticket_type_id IS NULL
   AND status = 'active' AND is_draft = FALSE
   AND fee_bps = 0 AND fixed_amount_cent IS NULL;

-- 说明:改造前已落库的核销流水按当时费率(0)计提,重结旧月份 pooled 支为 0。开发环境数据最后全删,
-- 不做回填(铁律:不为存量数据做兼容/回填设计);真实环境上线属另行部署决策。

-- ==========================================================================
-- ⑤ platform_retention_rules 停用(软删铁律:留库可查,不物理删)
-- ==========================================================================

-- 与综合服务费为「同一项、只扣一次」,故本表整体停用、并入 comprehensive_service_fee_configs。
-- 不 DROP TABLE:历史配置是「当年留存怎么配的」的决策痕迹,按软删铁律留库可查。
UPDATE platform_retention_rules SET status = 'inactive', updated_at = now() WHERE status = 'active';

COMMENT ON TABLE platform_retention_rules IS
    '【已停用·V370】原「项目公司留存比例」——与门票综合服务费为同一项、只扣一次(业务 HTML 口径),'
    '已并入 comprehensive_service_fee_configs(等价保值:留存率现值平移为全局默认费率)。'
    '本表留库仅作历史决策痕迹,读写两侧代码均已下线,不再参与任何计算。';

-- ==========================================================================
-- ⑥ 综合服务费收入流归属种子:归娱乐平台公司
-- ==========================================================================

-- HTML 明文「归娱乐平台公司所有」——归属不是运营的自由选项,种全局默认行(项目级仍可在
-- 结算配置中心覆盖,零硬编码);be_ent_platform 为 V354 落的主体骨架稳定 id。
INSERT INTO revenue_stream_attribution
    (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, is_draft, updated_by, updated_at)
SELECT 'rsa_ticket_service_fee_default', NULL, 'ticket_service_fee', 'be_ent_platform', 'entity',
       'active', FALSE, 'migration:V370', now()
 WHERE NOT EXISTS (SELECT 1 FROM revenue_stream_attribution
                    WHERE revenue_stream = 'ticket_service_fee'
                      AND project_id IS NULL AND status = 'active' AND is_draft = FALSE);

-- V375 通票分账瀑布重建 S8·第 2 刀:池无人可分的显式归属列 + 未核销款归属种子。
--
-- 【业务依据】(最终解释权=业务 HTML,逐字)
--   business-system-map-for-operations §「通票收入分账全流程」:
--     「没有被核销的权益,那部分钱归娱乐平台公司留存;游乐通票的未核销权益款归游乐公司。」
--   business-enum-quick-reference §「六业态收入归属」:通票收入 → 娱乐平台公司;游乐票 → 游乐公司。
--
-- ============================================================================
-- ① settlement_bills:第二段「无人可分」余额的显式归属(方案 §二十一.2 边界二/K18 ③④)
-- ============================================================================
--
-- 【业务场景】当月酒吧全走了固定规则(第二段没有参与者)、或当月压根没有酒吧参与分账时,
--   池里剩下的钱不能静默沉淀在「池 − 已分」的差里让财务月底自己猜——用户否掉的正是隐式残值。
--   这里是【显式归属 + 单列留痕 + 告警】:余额归娱乐平台公司(池的出资方,池链扣项 G/F 都归它),
--   金额与归属主体各占一列,对账页一眼可见「这个月有 X 元没分出去、归了谁」。
--
-- 【带符号】冲正月无人可分时余额可为负(= 由娱乐平台公司承接追回),照 V335/V370 先例不加 >= 0 CHECK。
-- 【与尾差的区别】rounding_residual_cent 是「按比例拆分四舍五入」产生的 ±几分;本列是「第二段没有
--   参与者」时的整段余额。两者都归平台兜底,但成因不同、量级不同,必须分列留痕。
ALTER TABLE settlement_bills ADD COLUMN IF NOT EXISTS pool_undistributed_cent BIGINT NOT NULL DEFAULT 0;
ALTER TABLE settlement_bills ADD COLUMN IF NOT EXISTS pool_undistributed_entity_id VARCHAR(64);

COMMENT ON COLUMN settlement_bills.pool_undistributed_cent IS
    'S8 两段分配边界(K18 ③④):第二段无参与者(排名档位酒吧为空/权重全 0)时的池余额,显式归娱乐平台公司留痕(带符号,冲正月可为负);正常两段分尽时恒 0。与舍入尾差(rounding_residual_cent)分列,成因不同';
COMMENT ON COLUMN settlement_bills.pool_undistributed_entity_id IS
    'pool_undistributed_cent 的归属主体快照(business_entities.id,解析自 revenue_stream_attribution 的 all_pass_ticket 流=娱乐平台公司;月结时定格)';

-- ============================================================================
-- ② 收入归属种子:通票收入 → 娱乐平台公司;游乐票 → 游乐公司
-- ============================================================================
--
-- HTML 明文口径,归属不是运营的自由选项(照 V370 种 ticket_service_fee 的先例种全局默认行,
-- 项目级仍可在结算配置中心覆盖,零硬编码)。这两条种子是 S8 未核销款分流的解析落点:
--   未核销权益款默认 → all_pass_ticket 流 → 娱乐平台公司(be_ent_platform);
--   游乐业态提供方的未核销权益款 → amusement_ticket 流 → 游乐公司(be_amusement)。
-- 同时使 settlement_bills.attributed_entity_id / 业态份额行的归属不再留白(仅归属标注,零金额影响)。
INSERT INTO revenue_stream_attribution
    (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, is_draft, updated_by, updated_at)
SELECT 'rsa_all_pass_ticket_default', NULL, 'all_pass_ticket', 'be_ent_platform', 'entity',
       'active', FALSE, 'migration:V375', now()
 WHERE NOT EXISTS (SELECT 1 FROM revenue_stream_attribution
                    WHERE revenue_stream = 'all_pass_ticket'
                      AND project_id IS NULL AND status = 'active' AND is_draft = FALSE);

INSERT INTO revenue_stream_attribution
    (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, is_draft, updated_by, updated_at)
SELECT 'rsa_amusement_ticket_default', NULL, 'amusement_ticket', 'be_amusement', 'entity',
       'active', FALSE, 'migration:V375', now()
 WHERE NOT EXISTS (SELECT 1 FROM revenue_stream_attribution
                    WHERE revenue_stream = 'amusement_ticket'
                      AND project_id IS NULL AND status = 'active' AND is_draft = FALSE);

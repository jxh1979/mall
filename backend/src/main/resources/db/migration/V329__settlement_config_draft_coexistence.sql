-- V329 结算配置中心批1(通票分账)-3:草案/生效【并存】底座(Path 2)。
--   缘由:批0(V326)只做「草案不生效」读闸 + 现存种子一次性转正;但单行 scope 表旧唯一索引只按 scope 唯一,
--         编辑落草案时草案行(is_draft=TRUE)会与生效行(is_draft=FALSE)撞唯一键 → 无法并存 → 编辑即冲掉生效值(破坏保值)。
--   本迁移把 6 张单行 scope 表的唯一索引【末尾追加 is_draft】,让「同 scope 的生效行 + 草案行」并存:
--         读侧 finder 恒读 is_draft=FALSE(生效行不变) → 编辑期间旧生效值仍被读到(保值);转正时换行(删旧生效+翻草案)。
--   ⚠️保值:新索引 = 旧索引列 + is_draft(超集 → 更宽松)。现存数据每 scope 至多一行,必满足新索引,建索引不会失败;
--          分账读值不变(finder 仍 is_draft=FALSE)。仅改索引 + 加两列,不改引擎算法/现有数值,可独立回滚。
--   范围:giveaway_cost_configs(D5 已废)不碰;ticket_distribution_rank_tiers(梯子多行·本无 scope 唯一索引)免改。
--   revenue_stream_attribution(§2)+ merchant_revenue_attribution_rules(§4,batch0)同升 Path 2:唯一索引亦尾加 is_draft。

-- ========== 1) 6 张单行 scope 表:唯一索引末尾追加 is_draft(草案/生效并存)==========
-- 均为 CREATE UNIQUE INDEX(非 ADD CONSTRAINT),故 DROP INDEX + 重建同名索引即可;原 scope 表达式原样保留,尾加 is_draft。
-- ⚠️偏离规约(§1.2 原写「只在末尾加 is_draft」):新索引额外加 WHERE status='active'(部分唯一)。缘由=软删铁律:
--   merchant_rate 软删=保留行 status→inactive(is_draft 不变=FALSE)。若索引不带 status,archived 的 is_draft=FALSE 行
--   仍占「同 scope 生效槽」,日后对同 scope 重配→转正翻 is_draft=FALSE 时会撞唯一(500)。加 WHERE status='active' 后
--   inactive 行不入索引→archived 与新生效行并存,与 revenue_stream_attribution(V319)既有部分唯一范式一致。
--   现存数据全为 status='active'(无 inactive),故部分谓词对现存约束无损、迁移安全。ON CONFLICT 须相应带 WHERE status='active'。

-- ① platform_retention_rules:旧 uq_prr (COALESCE(project_id,'__DEFAULT__'))
DROP INDEX IF EXISTS uq_prr;
CREATE UNIQUE INDEX IF NOT EXISTS uq_prr
    ON platform_retention_rules (COALESCE(project_id, '__DEFAULT__'), is_draft) WHERE status = 'active';

-- ② ticket_distribution_configs:旧 uq_tdc (COALESCE(project_id,'__DEFAULT__'))
DROP INDEX IF EXISTS uq_tdc;
CREATE UNIQUE INDEX IF NOT EXISTS uq_tdc
    ON ticket_distribution_configs (COALESCE(project_id, '__DEFAULT__'), is_draft) WHERE status = 'active';

-- ③ ticket_distribution_merchant_rates:旧 uq_tdmr (COALESCE(project_id,'__DEFAULT__'), merchant_id)
DROP INDEX IF EXISTS uq_tdmr;
CREATE UNIQUE INDEX IF NOT EXISTS uq_tdmr
    ON ticket_distribution_merchant_rates (COALESCE(project_id, '__DEFAULT__'), merchant_id, is_draft) WHERE status = 'active';

-- ④ comprehensive_service_fee_configs:旧 uq_csfc (COALESCE(project_id,'__DEFAULT__'), COALESCE(merchant_id,'__ANY__'), COALESCE(entitlement_type,'__ANY__'))
DROP INDEX IF EXISTS uq_csfc;
CREATE UNIQUE INDEX IF NOT EXISTS uq_csfc
    ON comprehensive_service_fee_configs (
        COALESCE(project_id, '__DEFAULT__'),
        COALESCE(merchant_id, '__ANY__'),
        COALESCE(entitlement_type, '__ANY__'),
        is_draft
    ) WHERE status = 'active';

-- ⑤ ticket_combo_discount_configs:旧 uq_tcdc (COALESCE(project_id,'__DEFAULT__'))
DROP INDEX IF EXISTS uq_tcdc;
CREATE UNIQUE INDEX IF NOT EXISTS uq_tcdc
    ON ticket_combo_discount_configs (COALESCE(project_id, '__DEFAULT__'), is_draft) WHERE status = 'active';

-- ⑥ channel_fee_configs(V327):旧 uq_cfc (COALESCE(project_id,'__DEFAULT__'), COALESCE(channel,'__ALL__'))
DROP INDEX IF EXISTS uq_cfc;
CREATE UNIQUE INDEX IF NOT EXISTS uq_cfc
    ON channel_fee_configs (COALESCE(project_id, '__DEFAULT__'), COALESCE(channel, '__ALL__'), is_draft) WHERE status = 'active';

-- ========== 2) revenue_stream_attribution:加 is_draft + 升 Path 2(草案/生效并存,同 6 表范式)==========
-- (复核修订:原批1 让归属走 Path 1 单行翻转→编辑期归属回落 null[D7=C 口径拿它当超额付款方]+草案被拒软删会永久覆盖旧归属不可回滚。改 Path 2。)
-- 现存行 = 当前生效归属 = 视同已批准,默认 is_draft=FALSE。读侧 resolveManagementEntity 保持 status='active' AND is_draft=FALSE。
ALTER TABLE revenue_stream_attribution ADD COLUMN IF NOT EXISTS is_draft BOOLEAN NOT NULL DEFAULT FALSE;
-- 软删铁律:归属条目下线 = status→inactive + archive_reason(不物理删)。
ALTER TABLE revenue_stream_attribution ADD COLUMN IF NOT EXISTS archive_reason TEXT;
-- 唯一索引升级:旧 uq_rsa_project_stream (scope, revenue_stream) WHERE status='active' → 尾加 is_draft(生效行+草案行并存)。
DROP INDEX IF EXISTS uq_rsa_project_stream;
CREATE UNIQUE INDEX IF NOT EXISTS uq_rsa_project_stream
    ON revenue_stream_attribution (COALESCE(project_id, '__DEFAULT__'), revenue_stream, is_draft) WHERE status = 'active';

-- ========== 3) ticket_distribution_merchant_rates:加 archive_reason(软删铁律)==========
-- 每客单价/固定额条目下线 = status→inactive + archive_reason。
ALTER TABLE ticket_distribution_merchant_rates ADD COLUMN IF NOT EXISTS archive_reason TEXT;

-- ========== 4) merchant_revenue_attribution_rules(batch0/analytics 域):唯一索引升级支持 Path 2 ==========
-- 待主会话协调 batch0:归因规则从 Path 1(同 id 翻转)升 Path 2(草案/生效并存);两条 partial 唯一索引尾加 is_draft。
-- 现存种子(V221 seed·V326 转正 is_draft=FALSE)每 scope 一行 active,新索引是旧超集+同谓词,建索引不失败、零行为变更。
DROP INDEX IF EXISTS uq_mrar_default_source;
CREATE UNIQUE INDEX IF NOT EXISTS uq_mrar_default_source
    ON merchant_revenue_attribution_rules (revenue_source, is_draft) WHERE rule_scope = 'default' AND status = 'active';
DROP INDEX IF EXISTS uq_mrar_merchant_source;
CREATE UNIQUE INDEX IF NOT EXISTS uq_mrar_merchant_source
    ON merchant_revenue_attribution_rules (merchant_id, revenue_source, is_draft) WHERE rule_scope = 'merchant' AND status = 'active';

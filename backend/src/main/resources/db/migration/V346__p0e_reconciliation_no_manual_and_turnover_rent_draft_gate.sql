-- V346（v7 审计 P0-E）两处止血，一号迁移：
--   ① 对账旁路手填封堵：integration_reconciliation_differences 增「登记人 + 取数来源 + 复核态」，
--      配合服务层「平台侧金额一律系统取数、请求体禁手填」——人工只能登记差异并进复核，不能改金额。
--   ② 抽成规则草案闸：turnover_rent_rules 纳入结算配置中心 Path 2「草案/生效并存 → 差人复核转正」范式。
-- 保值承诺：两块都只加列（带默认）+ 加索引 + 换 CHECK 白名单（只放宽不收紧），不改任何现有数值、不删行。

-- ══════════════ ① 对账差异：登记人 / 取数来源 / 复核态 ══════════════
-- 缘由：V030 建表时 internal_amount_cent（平台侧）与 provider_amount_cent（第三方侧）双双由请求体手填，
--       一名有 integration.reconciliation.manage 的操作员即可凭空造一条任意金额的「差异」，
--       且落库即 status='open' 可直接被处置——这是对账的旁路后门。
--       修后：第三方侧一律由 integration_order_syncs 系统取数；平台侧不接受任何输入；
--       人工登记的差异强制 status='needs_review'，且登记人不得自审（复核在服务层守卫）。

-- 登记人：人工登记差异时落操作者 id；系统自动对账产出的差异为 NULL（无人登记）。
ALTER TABLE integration_reconciliation_differences
    ADD COLUMN IF NOT EXISTS registered_by_user_id VARCHAR(64);

-- 取数来源：system=系统自动对账比对产出；manual_registration=人工登记（金额仍来自系统取数，仅「登记」这一动作是人工）。
-- 存量行一律 'system'（V030 起唯一写入方是自动对账与旧手填端点；旧手填行本就该被复核，不追溯改写，只做标注默认）。
ALTER TABLE integration_reconciliation_differences
    ADD COLUMN IF NOT EXISTS amount_source VARCHAR(32) NOT NULL DEFAULT 'system';

ALTER TABLE integration_reconciliation_differences
    DROP CONSTRAINT IF EXISTS chk_integration_recon_diffs_amount_source;
ALTER TABLE integration_reconciliation_differences
    ADD CONSTRAINT chk_integration_recon_diffs_amount_source
    CHECK (amount_source IN ('system', 'manual_registration'));

-- status 白名单增 'needs_review'（只放宽不收紧：原 5 态全部保留，存量行必满足）。
ALTER TABLE integration_reconciliation_differences
    DROP CONSTRAINT IF EXISTS chk_integration_recon_diffs_status;
ALTER TABLE integration_reconciliation_differences
    ADD CONSTRAINT chk_integration_recon_diffs_status
    CHECK (status IN ('open', 'needs_review', 'difference_found', 'matched', 'resolved', 'ignored'));

-- 待复核差异检索（复核队列按登记时间倒序）。
CREATE INDEX IF NOT EXISTS idx_integration_recon_diffs_needs_review
    ON integration_reconciliation_differences (status, updated_at DESC)
    WHERE status = 'needs_review';

-- ══════════════ ② 营业额抽成规则：草案 / 生效并存 + 复核转正 ══════════════
-- 缘由：turnover_rent_rules 建规则即 status='active'，读侧 findActiveTurnoverRentRuleByContract 立刻读到，
--       单人一次提交就改变下月抽成金额，无草案/无复核/无转正——与结算配置中心其余 10 类配置口径不一致。
--       修后：新建/改规则一律落草案（is_draft=TRUE），读侧只认 is_draft=FALSE，
--       转正走配置中心 POST /api/v1/settlement/config/turnover_rent_rule/{id}/activate（差人复核）。
-- 保值：存量行 is_draft 默认 FALSE = 当前生效规则视同已批准（同 V326/V329 对既有种子的处理口径），
--       抽成取数结果零变化。

ALTER TABLE turnover_rent_rules ADD COLUMN IF NOT EXISTS is_draft BOOLEAN NOT NULL DEFAULT FALSE;
-- updated_by：草案起草人 / 转正执行人。差人守卫（起草人 ≠ 转正人）读这一列。
ALTER TABLE turnover_rent_rules ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);
-- 软删铁律：规则下线 = status→inactive + archive_reason（不物理删）。
ALTER TABLE turnover_rent_rules ADD COLUMN IF NOT EXISTS archive_reason TEXT;

-- 不建唯一索引：抽成规则天然按合同多版本共存（effective_from 迭代），
-- 转正换行走 activateSingleRowDraft 的自连接（同 contract_id + merchant_id 的旧生效行软替换），不依赖唯一约束。
-- 只补检索索引：草案队列 / 生效规则查找。
CREATE INDEX IF NOT EXISTS idx_turnover_rent_rules_draft
    ON turnover_rent_rules (contract_id, is_draft, status);

-- ══════════════ 受限运行时授权（V296/V298/V338 教训：改表必查授权）══════════════
-- 加列不改所有权，但显式重授一遍，确保 complex_app 对本次涉及的三张表齐权（幂等，重复 GRANT 无副作用）。
GRANT SELECT, INSERT, UPDATE, DELETE ON integration_reconciliation_differences TO complex_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON turnover_rent_rules TO complex_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON turnover_rent_rule_tiers TO complex_app;

-- 文件版本：V0.01
-- V763（第十八批 CB 段第 398 条 398-R1）：通票分账通告期与非积分商户账单异议期改为版本化——改动落草案、经结算配置中心他人复核转正、
--   按生效起始日期（按营业日）由系统转正，旧生效行下线留库。
-- 依据：BSM:521「所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后按指定日期生效，
--   不即改即生效。」「无法判断是否涉及钱的，一律按涉及钱处理。」；ENUM:52「通告期为业务参数，后台可配，出厂默认一个月，须同时配置上下限。」；
--   BSM:1597「异议期长度由后台配置（业务参数，按项目可配，出厂默认 10 个工作日，须同时配置上下限）」。
-- 做法同 V725（第十八批 BQ 段 371-R1）：is_draft 草案位；status 加 scheduled（已批准、待生效）；草案槽与生效槽的部分唯一索引只约束 status = active。
-- effective_from 可空：既有行为空＝自始有效（研发阶段无存量，UD-07）；新提交的草案必填，须晚于该配置范围当前营业日。

-- ① 通告期：全平台一行（无范围列），出厂行 id 'default' 保留；草案 id 由幂等键派生，放宽长度。
ALTER TABLE ticket_distribution_notice_period DROP CONSTRAINT chk_tdnp_singleton;
ALTER TABLE ticket_distribution_notice_period ALTER COLUMN id DROP DEFAULT;
ALTER TABLE ticket_distribution_notice_period ALTER COLUMN id TYPE VARCHAR(48);
ALTER TABLE ticket_distribution_notice_period ADD COLUMN is_draft BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ticket_distribution_notice_period ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'active';
ALTER TABLE ticket_distribution_notice_period ADD CONSTRAINT chk_tdnp_status CHECK (status IN ('active', 'inactive', 'scheduled'));
ALTER TABLE ticket_distribution_notice_period ADD COLUMN effective_from DATE;
CREATE UNIQUE INDEX uq_tdnp_active_slot ON ticket_distribution_notice_period (is_draft) WHERE status = 'active';
COMMENT ON COLUMN ticket_distribution_notice_period.effective_from IS
    '生效起始日期（按平台默认营业日）：批准后待生效（status=scheduled），到日由系统转正；为空＝自始有效（既有行）。';

-- ② 异议期：按项目（'__global__' 为出厂行）；主键由 project_id 改为 id（既有行随默认值得号），每项目草案槽与生效槽各一。
ALTER TABLE settlement_objection_period_policy
    ADD COLUMN id VARCHAR(48) NOT NULL DEFAULT ('sopp_' || replace(gen_random_uuid()::text, '-', ''));
ALTER TABLE settlement_objection_period_policy DROP CONSTRAINT settlement_objection_period_policy_pkey;
ALTER TABLE settlement_objection_period_policy ADD PRIMARY KEY (id);
ALTER TABLE settlement_objection_period_policy ADD COLUMN is_draft BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE settlement_objection_period_policy ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'active';
ALTER TABLE settlement_objection_period_policy ADD CONSTRAINT chk_sopp_status CHECK (status IN ('active', 'inactive', 'scheduled'));
ALTER TABLE settlement_objection_period_policy ADD COLUMN effective_from DATE;
CREATE UNIQUE INDEX uq_sopp_active_slot ON settlement_objection_period_policy (project_id, is_draft) WHERE status = 'active';
COMMENT ON COLUMN settlement_objection_period_policy.effective_from IS
    '生效起始日期（按该项目营业日）：批准后待生效（status=scheduled），到日由系统转正；为空＝自始有效（既有行）。';

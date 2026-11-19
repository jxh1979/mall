-- 文件版本：V0.01
-- V682（第十八批 BA 段第 355 条 355-R1）：内部结算价每个版本填生效起始日期，批准后到日（按提供方营业日）才生效。
-- 依据：BSM:1671「价格变更形成新版本、指定生效时间并只影响后续业务」；BSM:521「所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期，
--   并先存为草案、经高敏权限“转正”审批通过后按指定日期生效，不即改即生效」；ENUM:187「全系统的钱账统一按「营业日」归集。」
-- effective_from 可空：V380 回填行与既有生效行为空＝自始有效（研发阶段无存量，UD-07）；新提交的草案必填，须晚于提供方当前营业日。
-- status 加 scheduled（已批准、待生效）：到生效起始日期由系统转正（旧生效行软替换 inactive、本行 active、同步四处业务快照）。
-- uq_ipc_code_slot 只约束 status = active，待生效行不占草案槽与生效槽，同一条目可有多条待生效版本。
ALTER TABLE internal_price_catalog ADD COLUMN effective_from DATE;
ALTER TABLE internal_price_catalog DROP CONSTRAINT chk_ipc_status;
ALTER TABLE internal_price_catalog ADD CONSTRAINT chk_ipc_status CHECK (status IN ('active', 'inactive', 'scheduled'));
COMMENT ON COLUMN internal_price_catalog.effective_from IS
    '生效起始日期（按提供方营业日）：批准后待生效（status=scheduled），到日由系统转正；为空＝自始有效（V380 回填与既有行）。';

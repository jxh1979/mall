-- 文件版本：V0.01
-- V641：第十八批 AQ 段第 234 条 234-R1（分红方案版本化）。
-- 依据：BSM:521「所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后按指定日期生效，
--   不即改即生效。新版本只对生效日之后发生的业务适用，已发生的业务一律按当时生效的版本计算；每一笔单据在生成时就把所用配置版本固化在单上」；
--   BSM:1775「必须以获批登记版本生效，修改不能追溯已经确认或发放的分红」；PGM:666「持股分红的方案配置……由最高权限审批，提交人不能审批自己的单据」。
-- dividend_plans 每行就是一个版本：active＝已批准、自生效起始日起照此出单；ended＝已批准、自生效起始日起停用；pending_review＝待最高权限审批；
-- rejected＝已驳回；expired＝审批通过时生效起始日已到、作废不用。stop_requested 记待审期间是否申请停用（批准时据此落 active 或 ended）。
-- 研发阶段无存量（UD-07），effective_from 直接设为必填。
ALTER TABLE dividend_plans DROP CONSTRAINT chk_dp_status;
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_status
    CHECK (status IN ('pending_review', 'active', 'ended', 'rejected', 'expired'));
ALTER TABLE dividend_plans ADD COLUMN stop_requested boolean NOT NULL DEFAULT false;
ALTER TABLE dividend_plans ADD COLUMN reason varchar(1000);
ALTER TABLE dividend_plans ADD COLUMN review_id varchar(80);
ALTER TABLE dividend_plans ADD COLUMN decided_by varchar(80);
ALTER TABLE dividend_plans ADD COLUMN decided_at timestamptz;
ALTER TABLE dividend_plans ALTER COLUMN effective_from SET NOT NULL;

-- 同商户可有多个已批准版本，按生效起始日排开；同一生效起始日只能有一个已批准版本，同时至多一个待审批版本。
DROP INDEX IF EXISTS uq_dividend_plans_active_merchant;
CREATE UNIQUE INDEX uq_dividend_plans_effective
    ON dividend_plans (merchant_id, effective_from)
 WHERE status IN ('active', 'ended');
CREATE UNIQUE INDEX uq_dividend_plans_pending
    ON dividend_plans (merchant_id)
 WHERE status = 'pending_review';

-- 生产受限运行时权限：新增列由既有表级 GRANT 覆盖，此处幂等重授兜底（照 V303）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_plans TO complex_app';
    END IF;
END $$;

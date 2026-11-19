-- 文件版本：V0.01
-- V659：第十八批 AX 段第 272 条 272-R1（分红方案平台（参股方）比例上下限）。
-- 依据：BSM:1775「新酒吧可预填 80%／20%（业务参数，按经营主体可配，须同时配置上下限）建议值，但必须以获批登记版本生效」；ENUM:297；PGM:436；
--   BSM:521「超出该区间的配置一律拒绝生效，未填写上下限的参数不得上线」；决策台账 DEC-R3-2026-08-04-30「具体数值留到落地时逐个定」。
-- 照 V642（244-R1 分红周期上下限）：上下限按万分比列在方案版本行上、随版本一起审批（234-R1）；新建必填、平台比例须落在上下限之内由服务层校验；
-- 不预置出厂数值。研发阶段无存量（UD-07），不回填。
ALTER TABLE dividend_plans ADD COLUMN IF NOT EXISTS platform_share_min_bps int;
ALTER TABLE dividend_plans ADD COLUMN IF NOT EXISTS platform_share_max_bps int;
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_share_bounds CHECK ((platform_share_min_bps IS NULL AND platform_share_max_bps IS NULL)
    OR (platform_share_min_bps IS NOT NULL AND platform_share_max_bps IS NOT NULL
        AND 0 <= platform_share_min_bps AND platform_share_min_bps <= platform_share_max_bps AND platform_share_max_bps <= 10000));

-- 生产受限运行时权限：新增列由既有表级 GRANT 覆盖，此处幂等重授兜底（照 V642）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_plans TO complex_app';
    END IF;
END $$;

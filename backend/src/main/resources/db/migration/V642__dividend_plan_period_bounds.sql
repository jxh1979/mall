-- 文件版本：V0.01
-- V642：第十八批 AQ 段第 244 条 244-R1（分红周期上下限）。
-- 依据：ENUM:299「分红周期可配置（业务参数，按经营主体可配，酒吧商户作为独立经营主体同样适用，须同时配置上下限）」；
--   BSM:521「超出该区间的配置一律拒绝生效，未填写上下限的参数不得上线」；BSM:1777「分红周期一律取月及月的整数倍粒度（如月、季、半年、年），不使用周粒度」；
--   决策台账 DEC-R3-2026-08-04-30「全部可配参数须同时写明取值上下限，超区间拒绝生效，未填上下限的参数不得上线（具体数值留到落地时逐个定）」、
--   DEC-P1I-01（三份 HTML 区间型可配参数括注补「须同时配置上下限」）。
-- 上下限按月数、与周期同一粒度（1、3、6、12＝月、季、半年、年），列在方案版本行上、随版本一起审批（234-R1）；新建必填、周期须落在上下限之内由服务层校验；
-- 不预置出厂数值。研发阶段无存量（UD-07），不回填。
ALTER TABLE dividend_plans ADD COLUMN period_min_months SMALLINT;
ALTER TABLE dividend_plans ADD COLUMN period_max_months SMALLINT;
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_period_min_months CHECK (period_min_months IN (1, 3, 6, 12));
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_period_max_months CHECK (period_max_months IN (1, 3, 6, 12));
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_period_bounds CHECK (period_min_months <= period_max_months);

-- 生产受限运行时权限：新增列由既有表级 GRANT 覆盖，此处幂等重授兜底（照 V303）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_plans TO complex_app';
    END IF;
END $$;

-- 文件版本：V0.01
-- V658：第十八批 AX 段第 271 条 271-R1（全平台统一分红代扣个税税率版本）。
-- 依据：BSM:1776「其分得的款项按「股息红利」计税，由发放的经营主体代扣个人所得税（安全阈值，全平台统一可配，调整须经财务、税务、法务三审并由
--   顶层平台公司最终确认，出厂默认 20%，须同时配置上下限），发放时在单据上写清代扣了多少」「税率以参数形式配置，不在业务页面硬编码」；
--   ENUM:298；BSM:521「每一笔单据在生成时就把所用配置版本固化在单上」「还必须写明取值的上限与下限，超出该区间的配置一律拒绝生效，未填写上下限的参数
--   不得上线」；决策台账 DEC-R8-82「调整须走最高一级审批与三审留痕」、DEC-R3-2026-08-04-30「具体数值留到落地时逐个定」。
-- ① dividend_tax_rate_versions：全平台一套版本。status：pending_review 财务、税务、法务三审与最高确认中（finance.tax_rule.adjust 复核链，
--    DividendTaxRateService）→ approved 已生效 / rejected 已驳回。effective_from 为空＝出厂版本自始有效；新版本必填生效起始日期与上下限，
--    下限 ≤ 税率 ≤ 上限。同一生效起始日至多一条待审或已生效版本。出单取出单当日营业日已生效的版本（生效日不晚于该日的最新已批准版本，出厂版本排最后）。
-- ② 出厂版本 dtrv_factory：20%，上下限不预置（照 V499、V508）。
-- ③ dividend_statements.tax_rate_version_id：计算单固化所用税率版本；tax_rate_bps 照旧快照生效税率（公司股东主理人为 0）。
-- ④ dividend_plans 去掉方案级代扣税率列（税率不再按方案配）。研发阶段无存量（UD-07），无视图引用。
CREATE TABLE IF NOT EXISTS dividend_tax_rate_versions (
    id varchar(64) PRIMARY KEY,
    rate_bps int NOT NULL,
    min_bps int,
    max_bps int,
    effective_from date,
    status varchar(24) NOT NULL,
    reason text NOT NULL,
    review_id varchar(80),
    created_by varchar(80),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_dtrv_status CHECK (status IN ('pending_review', 'approved', 'rejected')),
    CONSTRAINT chk_dtrv_rate CHECK (rate_bps BETWEEN 0 AND 10000),
    CONSTRAINT chk_dtrv_bounds CHECK ((min_bps IS NULL AND max_bps IS NULL)
        OR (min_bps IS NOT NULL AND max_bps IS NOT NULL AND 0 <= min_bps AND min_bps <= rate_bps AND rate_bps <= max_bps AND max_bps <= 10000))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_dtrv_effective ON dividend_tax_rate_versions (COALESCE(effective_from, DATE '1900-01-01'))
    WHERE status IN ('pending_review', 'approved');

INSERT INTO dividend_tax_rate_versions (id, rate_bps, min_bps, max_bps, effective_from, status, reason)
VALUES ('dtrv_factory', 2000, NULL, NULL, NULL, 'approved', '出厂默认 20%（BSM:1776）')
ON CONFLICT (id) DO NOTHING;

COMMENT ON TABLE dividend_tax_rate_versions IS '全平台统一分红代扣个税税率版本（BSM:1776；经财务、税务、法务三审与最高确认后自生效起始日起用，出单固化所用版本）';

ALTER TABLE dividend_statements ADD COLUMN IF NOT EXISTS tax_rate_version_id varchar(64);
ALTER TABLE dividend_plans DROP COLUMN IF EXISTS tax_rate_bps;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_tax_rate_versions TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_statements TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_plans TO complex_app';
    END IF;
END $$;

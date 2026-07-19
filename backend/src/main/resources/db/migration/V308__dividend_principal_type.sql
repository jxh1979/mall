-- V308 主理人类型（个人/公司）→ 代扣个税分支（2026-07-19 用户拍板 #28）。
-- 口径：主理人有类型【个人 individual / 公司 company】。
--   个人股东（自然人真实持股）→ 按股息红利代扣个税（税率沿用 dividend_plans.tax_rate_bps，默认 20%）。
--   公司股东（法人持股）→ 不代扣个税、按约定全额发放（发放单据代扣额 tax_cent=0，全额进主理人应得）。
-- 类型是主理人（持股关系）的属性，落方案配置（方案=商户×主理人维度）；历史分红单按单内类型快照追溯。
-- 复用现有 tax_rate_bps（公司只是把生效税率按类型强制为 0，不重复建税率字段）。

-- ── 1. 方案配置：主理人类型 ──────────────────────────────────────────────
ALTER TABLE dividend_plans
    ADD COLUMN IF NOT EXISTS principal_type varchar(16) NOT NULL DEFAULT 'individual';
ALTER TABLE dividend_plans DROP CONSTRAINT IF EXISTS chk_dp_principal_type;
ALTER TABLE dividend_plans ADD CONSTRAINT chk_dp_principal_type
    CHECK (principal_type IN ('individual', 'company'));

-- ── 2. 计算单快照：主理人类型 + 公司零税恒等 ──────────────────────────────
-- 快照当期主理人类型（历史单永远按当期口径追溯，改类型不改历史单）；tax_rate_bps 已快照生效税率
-- （公司=0），tax_cent 由 V299 chk_ds_tax_nonneg 约束在 [0, principal_gross]。此处补硬恒等：
-- 公司股东单代扣额必须为 0（DB 层杜绝公司单被算出非零代扣）。
ALTER TABLE dividend_statements
    ADD COLUMN IF NOT EXISTS principal_type varchar(16) NOT NULL DEFAULT 'individual';
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_principal_type;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_principal_type
    CHECK (principal_type IN ('individual', 'company'));
ALTER TABLE dividend_statements DROP CONSTRAINT IF EXISTS chk_ds_company_no_tax;
ALTER TABLE dividend_statements ADD CONSTRAINT chk_ds_company_no_tax
    CHECK (principal_type <> 'company' OR tax_cent = 0);

-- ── 3. 生产受限运行时权限（V298/V299/V303/V304 教训：新列一律再 GRANT complex_app 兜底）──
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_plans TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_statements TO complex_app';
    END IF;
END $$;

-- P0#7 对账差异强制化（finance §6C / R6-D07 确认）。
-- 现状证据：
--   · V008 建 finance_reconciliation_differences 仅有 difference_type/business_ref/amount_cent/status，
--     无 expected/actual/source 列——差异可被纯手填，平台侧 expected 无系统取数约束。
--   · V069 的 chk_frd_difference_type 为兼容既有测试，CHECK 取值集混入了遗留值 'amount_mismatch'
--     （语义=amount），尚未纯化为规格四类。
-- 本迁移收口（R6-D07）：
--   1) 补 expected_amount_cent / actual_amount_cent / source(system|manual) 三列；
--   2) 历史 'amount_mismatch' 先 UPDATE 归一为 'amount'，再把 difference_type 收紧为四类
--      (amount/count/status/missing) 的严格 CHECK；
--   3) source 取值 CHECK 收紧为 system|manual：平台侧 expected 由系统取数（source=system），
--      第三方侧未接入前 source=manual 进复核，纯手填差异（无系统取数支撑）由服务层拒绝。

-- ── 1) 补列：平台侧 expected（系统取数）、第三方侧 actual、取数来源 ──────────────
ALTER TABLE finance_reconciliation_differences
    ADD COLUMN IF NOT EXISTS expected_amount_cent BIGINT,
    ADD COLUMN IF NOT EXISTS actual_amount_cent BIGINT,
    ADD COLUMN IF NOT EXISTS source VARCHAR(16) NOT NULL DEFAULT 'system';

-- ── 2) difference_type 纯化：先归一遗留值，再收紧 CHECK 到规格四类 ───────────────
-- 先把历史/遗留 'amount_mismatch' 归一为 'amount'（语义一致），保证收紧 CHECK 不被既有数据卡住。
UPDATE finance_reconciliation_differences
   SET difference_type = 'amount'
 WHERE difference_type = 'amount_mismatch';

-- 替换 V069 的宽松 CHECK（含遗留值）为规格四类严格 CHECK。
ALTER TABLE finance_reconciliation_differences
    DROP CONSTRAINT IF EXISTS chk_frd_difference_type;

ALTER TABLE finance_reconciliation_differences
    ADD CONSTRAINT chk_frd_difference_type CHECK (difference_type IN (
        'amount', 'count', 'status', 'missing'
    ));

-- ── 3) source 取值收紧：system（平台侧系统取数）| manual（第三方侧未接入手工录入进复核） ──
ALTER TABLE finance_reconciliation_differences
    DROP CONSTRAINT IF EXISTS chk_frd_source;

ALTER TABLE finance_reconciliation_differences
    ADD CONSTRAINT chk_frd_source CHECK (source IN ('system', 'manual'));

-- ── 4) seed 权限（参照 V008 finance 权限 INSERT 模式）──────────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.reconciliation.difference.create', 'Create finance reconciliation differences', 'finance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'finance.reconciliation.difference.create'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

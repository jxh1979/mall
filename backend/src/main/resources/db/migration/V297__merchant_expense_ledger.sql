-- V297 商户费用归集台账（持股分红净利润制前置数据基建之二，2026-07-18 拍板）。
-- 目标：把「这家店这个月花了多少钱」归集成台账，供分红净利润基数扣减（净利 = 毛利 − 费用）。
-- 来源三类：auto_energy（能耗费，挂接 ar_ap_items source_type='energy_fee' 应收）、
--   auto_property（物业费，挂接 property_fee_bills 计费留痕）、manual（人工/摊销/其他，后台录入，理由必填）。
-- 分摊规则（公共费用按面积/营收比例）明确留二期，source 字段已预留区分。
-- 口径：资金类仅追加——更正走红字冲销新记录（负金额 + source_ref=原记录 id），不改历史行；
--   纳入 V140 complex_block_mutation() append-only 硬强制（immutable：禁 UPDATE + DELETE）。

CREATE TABLE IF NOT EXISTS merchant_expense_entries (
    id varchar(120) PRIMARY KEY,
    merchant_id varchar(64) NOT NULL,
    period varchar(16) NOT NULL,
    expense_type varchar(32) NOT NULL,
    amount_cent bigint NOT NULL,
    source varchar(32) NOT NULL,
    -- auto_*：上游单据 id（ar_ap_items.id / property_fee_bills.id）；manual 冲销：原台账行 id；manual 正常录入：NULL。
    source_ref varchar(120),
    note varchar(1000),
    operator_user_id varchar(80),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_mex_period CHECK (period ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT chk_mex_expense_type CHECK (expense_type IN
        ('rent_allocation', 'energy', 'property_fee', 'labor', 'amortization', 'other')),
    CONSTRAINT chk_mex_source CHECK (source IN ('auto_energy', 'auto_property', 'manual')),
    CONSTRAINT chk_mex_amount_nonzero CHECK (amount_cent <> 0),
    -- 自动挂接行必须携带上游单据 id（幂等去重锚点）。
    CONSTRAINT chk_mex_auto_source_ref CHECK (source = 'manual' OR source_ref IS NOT NULL)
);

-- 自动挂接幂等：同一上游单据只计入一次（id 亦由上游单据派生，双保险）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_mex_auto_source_ref
    ON merchant_expense_entries (source, source_ref)
 WHERE source <> 'manual';

-- 期间汇总 / 台账筛选主索引（分红引擎费用侧取数点：商户 + 月份 + 类型）。
CREATE INDEX IF NOT EXISTS idx_mex_merchant_period_type
    ON merchant_expense_entries (merchant_id, period, expense_type);

-- append-only 硬强制：纯追加台账（更正走红字冲销新行），禁 UPDATE 与 DELETE（V140 immutable 口径）。
DO $$
BEGIN
    IF to_regclass('merchant_expense_entries') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_append_only_ud ON merchant_expense_entries';
        EXECUTE 'CREATE TRIGGER trg_append_only_ud BEFORE UPDATE OR DELETE ON merchant_expense_entries '
             || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()';
    END IF;
END $$;

-- 生产受限运行时 role 纵深：与 V140 immutable 表口径一致，REVOKE UPDATE + DELETE（role 不存在则跳过）。
DO $$
BEGIN
    IF to_regclass('merchant_expense_entries') IS NOT NULL
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'REVOKE UPDATE, DELETE ON merchant_expense_entries FROM complex_app';
    END IF;
END $$;

-- 权限种子（V279 范式）：view=台账/汇总只读；manage=手工录入 + 红字冲销 + 自动挂接触发（高危：影响分红基数）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.merchant_expense.view', 'View merchant expense ledger and period summary', 'finance', 'medium', FALSE, 'active'),
    ('finance.merchant_expense.manage', 'Manage merchant expense ledger (manual entry, reversal, auto attach)', 'finance', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 平台管理员直授（V206 范式）。其它角色按需另行授权。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', code
  FROM (VALUES ('finance.merchant_expense.view'), ('finance.merchant_expense.manage')) AS seed(code)
ON CONFLICT (role_id, permission_code) DO NOTHING;

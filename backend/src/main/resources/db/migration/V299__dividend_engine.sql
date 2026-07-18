-- V299 持股分红引擎（2026-07-18 拍板：净利润制 / 真实股东股息红利 20% 代扣 / 季度正式+月度预览）。
-- 三表：
--   dividend_plans           分红方案配置（商户/主理人维度，凡涉数据皆可配：比例/税率/周期/基数构成开关；
--                            80/20、20% 税率、季度周期仅为默认值，可后台调整，改动走审计事件留痕）。
--   dividend_statements      期间分红计算单（快照头：配置快照 + 算式结果 + 状态机；金额恒等由 DB CHECK 硬保证；
--                            重算=旧单置 voided + 新单落表（红字更正范式），审批后不可重算）。
--   dividend_statement_lines 算式明细行（append-only 硬冻结：V140 complex_block_mutation 禁 UPDATE/DELETE；
--                            每行留痕可追溯到三个来源台账——营收流水 / V296 出库成本汇总 / V297 费用归集台账）。

-- ── 1. 分红方案配置 ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dividend_plans (
    id varchar(64) PRIMARY KEY,
    merchant_id varchar(64) NOT NULL,
    principal_user_id varchar(80) NOT NULL,
    principal_name varchar(200),
    platform_share_bps int NOT NULL DEFAULT 8000,
    tax_rate_bps int NOT NULL DEFAULT 2000,
    period_type varchar(16) NOT NULL DEFAULT 'quarterly',
    -- 基数构成开关（凡涉数据皆可配）：计入的收入域（csv：bar,ticket）/ 是否扣销货成本 / 计入的费用类别（csv）。
    revenue_domains varchar(200) NOT NULL DEFAULT 'bar,ticket',
    include_inventory_cost boolean NOT NULL DEFAULT TRUE,
    expense_types varchar(300) NOT NULL DEFAULT 'rent_allocation,energy,property_fee,labor,amortization,other',
    effective_from date,
    status varchar(16) NOT NULL DEFAULT 'active',
    note varchar(1000),
    updated_by varchar(80),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_dp_platform_share_bps CHECK (platform_share_bps BETWEEN 0 AND 10000),
    CONSTRAINT chk_dp_tax_rate_bps CHECK (tax_rate_bps BETWEEN 0 AND 10000),
    CONSTRAINT chk_dp_period_type CHECK (period_type IN ('quarterly', 'monthly')),
    CONSTRAINT chk_dp_status CHECK (status IN ('active', 'ended'))
);

-- 一个商户同一时期只允许一个生效中的分红方案（设计稿 §2）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_dividend_plans_active_merchant
    ON dividend_plans (merchant_id)
 WHERE status = 'active';

-- ── 2. 期间分红计算单（快照头 + 状态机）────────────────────────────────────
CREATE TABLE IF NOT EXISTS dividend_statements (
    id varchar(64) PRIMARY KEY,
    merchant_id varchar(64) NOT NULL,
    plan_id varchar(64) NOT NULL REFERENCES dividend_plans(id),
    kind varchar(16) NOT NULL,
    period_label varchar(16) NOT NULL,
    period_start date NOT NULL,
    period_end date NOT NULL,
    -- 配置快照（历史单永远按当期生效口径追溯，改方案不改历史单）。
    principal_user_id varchar(80) NOT NULL,
    platform_share_bps int NOT NULL,
    tax_rate_bps int NOT NULL,
    revenue_domains varchar(200) NOT NULL,
    include_inventory_cost boolean NOT NULL,
    expense_types varchar(300) NOT NULL,
    -- 算式结果（整数分；恒等式由下方 CHECK 硬保证）。
    revenue_cent bigint NOT NULL,
    cost_cent bigint NOT NULL,
    expense_cent bigint NOT NULL,
    net_profit_cent bigint NOT NULL,
    distributable_cent bigint NOT NULL,
    platform_share_cent bigint NOT NULL,
    principal_gross_cent bigint NOT NULL,
    tax_cent bigint NOT NULL,
    principal_net_cent bigint NOT NULL,
    -- V296 口径的诚实边界：窗口内无成本基准出库笔数（>0 提示成本合计或有低估）。
    no_cost_basis_count bigint NOT NULL DEFAULT 0,
    status varchar(24) NOT NULL,
    reject_reason varchar(1000),
    rejected_by varchar(80),
    rejected_at timestamptz,
    ar_ap_item_id varchar(64),
    computed_by varchar(80),
    submitted_by varchar(80),
    submitted_at timestamptz,
    approved_by varchar(80),
    approved_at timestamptz,
    paid_by varchar(80),
    paid_at timestamptz,
    voided_by varchar(80),
    voided_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_ds_kind CHECK (kind IN ('official', 'preview')),
    CONSTRAINT chk_ds_status CHECK (status IN
        ('preview', 'draft', 'pending_review', 'approved', 'paid', 'rejected', 'voided')),
    CONSTRAINT chk_ds_share_bps CHECK (platform_share_bps BETWEEN 0 AND 10000),
    CONSTRAINT chk_ds_tax_bps CHECK (tax_rate_bps BETWEEN 0 AND 10000),
    -- 数字恒等（三来源→净利→劈分→税→净发放，DB 层硬保证不可落账不恒等的单）。
    CONSTRAINT chk_ds_net_identity CHECK (net_profit_cent = revenue_cent - cost_cent - expense_cent),
    CONSTRAINT chk_ds_distributable CHECK (distributable_cent = GREATEST(net_profit_cent, 0)),
    CONSTRAINT chk_ds_split_identity CHECK (platform_share_cent + principal_gross_cent = distributable_cent),
    CONSTRAINT chk_ds_net_pay_identity CHECK (principal_net_cent = principal_gross_cent - tax_cent),
    CONSTRAINT chk_ds_tax_nonneg CHECK (tax_cent >= 0 AND tax_cent <= principal_gross_cent)
);

-- 重跑作业幂等：同期同商户同类别只允许一张未作废计算单（重算=旧单 voided + 新单）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_dividend_statements_period
    ON dividend_statements (merchant_id, kind, period_label)
 WHERE status <> 'voided';

CREATE INDEX IF NOT EXISTS idx_dividend_statements_merchant_period
    ON dividend_statements (merchant_id, period_label, status);

-- ── 3. 算式明细行（append-only）────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dividend_statement_lines (
    id varchar(120) PRIMARY KEY,
    statement_id varchar(64) NOT NULL REFERENCES dividend_statements(id),
    merchant_id varchar(64) NOT NULL,
    seq int NOT NULL,
    line_type varchar(40) NOT NULL,
    amount_cent bigint NOT NULL,
    detail varchar(1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_dsl_line_type CHECK (line_type IN
        ('revenue_bar', 'revenue_bar_refund', 'revenue_ticket', 'revenue_ticket_refund',
         'cost_inventory', 'expense_rent_allocation', 'expense_energy', 'expense_property_fee',
         'expense_labor', 'expense_amortization', 'expense_other',
         'net_profit', 'platform_share', 'principal_gross', 'tax_withheld', 'principal_net'))
);

CREATE INDEX IF NOT EXISTS idx_dsl_statement ON dividend_statement_lines (statement_id, seq);

-- append-only 硬强制（V140 口径）：算式明细一经落表不改不删，更正走整单 voided + 重算新单。
DO $$
BEGIN
    IF to_regclass('dividend_statement_lines') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_append_only_ud ON dividend_statement_lines';
        EXECUTE 'CREATE TRIGGER trg_append_only_ud BEFORE UPDATE OR DELETE ON dividend_statement_lines '
             || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()';
    END IF;
END $$;

-- ── 4. 生产受限运行时权限（V298 教训：新表必须显式 GRANT 给 complex_app，否则生产 500）──
-- 口径：方案配置可改（S/I/U，无 DELETE，停用走 status='ended'）；
--       计算单头状态机需 UPDATE（金额列冻结由服务层仓储只更新状态/审批列保证），无 DELETE（作废走 voided）；
--       算式明细仅追加（S/I），并与 V140 immutable 口径一致 REVOKE UPDATE/DELETE。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_plans TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_statements TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT ON dividend_statement_lines TO complex_app';
        EXECUTE 'REVOKE UPDATE, DELETE ON dividend_statement_lines FROM complex_app';
    END IF;
END $$;

-- ── 5. 租金计入费用台账（2026-07-18 用户拍板）────────────────────────────
-- V297 chk_mex_source 放宽：新增 auto_rent（租金自动挂接：contract_rent 合同租金 + turnover_rent 营业额抽成）。
-- expense_type 白名单已含 rent_allocation，无需改动。
ALTER TABLE merchant_expense_entries DROP CONSTRAINT IF EXISTS chk_mex_source;
ALTER TABLE merchant_expense_entries ADD CONSTRAINT chk_mex_source
    CHECK (source IN ('auto_energy', 'auto_property', 'auto_rent', 'manual'));

-- ── 6. 权限种子（V279 范式）──────────────────────────────────────────────
-- view=方案/计算单只读（商户 scope 账号被后端强制只看本商户）；manage=方案配置+算单+审批+发放（高危）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.dividend.view', '查看持股分红（方案与分红单）', 'finance', 'medium', FALSE, 'active'),
    ('finance.dividend.manage', '管理持股分红（方案配置/算单/审批/发放）', 'finance', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 平台管理员直授（V206 范式）。审批动作要求 manage，当前仅 role_platform_admin 持有=最高权限审批现状口径；
-- 商户专区主理人只读视图按需另授 view（走既有商户 scope 硬闸）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', code
  FROM (VALUES ('finance.dividend.view'), ('finance.dividend.manage')) AS seed(code)
ON CONFLICT (role_id, permission_code) DO NOTHING;

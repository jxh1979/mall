-- V217 个税税表可配置化(治理铁律:业务策略参数禁止硬编码)。
--   原:closePayrollMonth(JdbcStaffRepository)把劳务报酬预扣个税税表写死在 SQL CASE WHEN 里
--       (应税基数80% / 档位40万·200万·500万分 / 税率20·30·40% / 速算扣除20万·70万分 / 减除费用800元)。
--   改:税表落配置表 payroll_tax_brackets,按毛收入(cent)分档,每档完整表达税额公式:
--       税额 = GREATEST(0, (svc * income_base_bps/10000 - flat_deduction_cent) * rate_bps/10000 - quick_deduction_cent)
--     SQL 改为 JOIN 落档取参数,不再写死数值。种子=原 CASE WHEN 逐分等价(见下 4 行)。
--   税法参数变更→改配置不改代码。整数运算保持与原 SQL 一致(先乘后除、Java/PG 整除截断)。

CREATE TABLE IF NOT EXISTS payroll_tax_brackets (
    id                  VARCHAR(48) PRIMARY KEY,
    category            VARCHAR(32)  NOT NULL,          -- 税目;当前唯一:'labor_remuneration'(劳务报酬预扣预缴)
    seq                 INT          NOT NULL,          -- 档位序(1..n,便于展示排序)
    income_floor_cent   BIGINT       NOT NULL,          -- 毛收入下界(含),单位分
    income_ceiling_cent BIGINT,                         -- 毛收入上界(含),单位分;NULL=最高档无上界
    income_base_bps     INT          NOT NULL,          -- 应税基数占毛收入的万分比(8000=80%,10000=100%)
    flat_deduction_cent BIGINT       NOT NULL DEFAULT 0,-- 定额费用减除(分):档1的800元=80000分;>4000元档用比例(=0)
    rate_bps            INT          NOT NULL,          -- 税率(万分比;2000=20%)
    quick_deduction_cent BIGINT      NOT NULL DEFAULT 0,-- 速算扣除(分)
    effective_from      DATE,                           -- 生效起(可空=一直有效);预留税法调整
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_ptb_base_bps  CHECK (income_base_bps > 0 AND income_base_bps <= 10000),
    CONSTRAINT chk_ptb_rate_bps  CHECK (rate_bps >= 0 AND rate_bps <= 10000),
    CONSTRAINT chk_ptb_floor     CHECK (income_floor_cent >= 0),
    CONSTRAINT chk_ptb_ceiling   CHECK (income_ceiling_cent IS NULL OR income_ceiling_cent >= income_floor_cent)
);

-- 种子=原 closePayrollMonth CASE WHEN 逐分等价(劳务报酬预扣,单位分):
--   档1 0<svc<=400000     : (svc-80000)*20%           → base100% 定额减80000 税率20% 速扣0
--   档2 400000<svc<=2500000: svc*80%*20%              → base80%  定额0     税率20% 速扣0        (svc*0.8<=2000000)
--   档3 2500000<svc<=6250000: svc*80%*30% - 200000    → base80%  定额0     税率30% 速扣200000   (2000000<svc*0.8<=5000000)
--   档4 svc>6250000        : svc*80%*40% - 700000      → base80%  定额0     税率40% 速扣700000   (svc*0.8>5000000)
INSERT INTO payroll_tax_brackets
    (id, category, seq, income_floor_cent, income_ceiling_cent, income_base_bps, flat_deduction_cent, rate_bps, quick_deduction_cent)
VALUES
    ('ptb_labor_1', 'labor_remuneration', 1,       1,  400000, 10000, 80000, 2000,      0),
    ('ptb_labor_2', 'labor_remuneration', 2,  400001, 2500000,  8000,     0, 2000,      0),
    ('ptb_labor_3', 'labor_remuneration', 3, 2500001, 6250000,  8000,     0, 3000, 200000),
    ('ptb_labor_4', 'labor_remuneration', 4, 6250001,    NULL,  8000,     0, 4000, 700000)
ON CONFLICT (id) DO NOTHING;

-- 税表维护权限(高敏:决定代扣个税口径)。读复用 staff.payroll.manage,写用本权限;仿 V211 范式。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('staff.tax_bracket.manage', 'Maintain payroll tax brackets (labor remuneration withholding table)', 'staff', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'staff.tax_bracket.manage'
ON CONFLICT (role_id, permission_code) DO NOTHING;

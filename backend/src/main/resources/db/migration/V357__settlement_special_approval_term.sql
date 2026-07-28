-- V357 S9 短期激励期限:超池特批加【生效期】+ 期限后台可配(默认 3 个月) + 到期回收。
--   口径(业务 HTML 逐字·最终解释权):「若因短期激励需要突破可分配池上限,仅限平台负责人特批的短期激励活动
--   (期限可配,默认不超过 3 个月)」(business-system-map §通票收入分账全流程);
--   「以上所有金额、比例、封顶、档位、期限均后台可配,禁止写死」(同段 + business-enum-quick-reference)。
--
-- v7 审计 R-2 同批落实:超池特批改用【专属权限点】settlement.bill.special_approve(高敏·需审批·仅授平台负责人),
--   与常规过账 settlement.bill.post 分权——过账可下放财务运营,超池特批只能平台负责人本人批。
--
-- 到期回收 = 【惰性判定】,不建定时作业(见 PRD §5.0):特批过期后
--   ① 月结重跑不再冻结该单(引擎按新算态覆盖,仍超池即打回 draft 待重新特批);
--   ② 过账 postBill 对超池单拒绝(SPECIAL_APPROVAL_EXPIRED)。
--   过期特批引用不清空(留痕保全),失效靠有效期判定,不靠删数据。
--
-- 零变更保证:无特批单(special_approval_ref IS NULL)时本迁移不改任何金额列、不改任何判定分支。
-- 本迁移只加两列 + 一个权限点,不改现有表数据、可独立回滚。

-- ========== ① settlement_bills:特批生效期(强制字段) ==========
ALTER TABLE settlement_bills
    ADD COLUMN IF NOT EXISTS special_approval_valid_until DATE;

COMMENT ON COLUMN settlement_bills.special_approval_valid_until IS
    '超池特批到期日(含);到期即失效:过账拒绝、月结重跑不再冻结该单。有特批引用必有到期日(chk_sb_special_approval_term)';

-- R-2「生效期强制字段」:有特批引用就必须有到期日,禁止无限期特批。
ALTER TABLE settlement_bills DROP CONSTRAINT IF EXISTS chk_sb_special_approval_term;
ALTER TABLE settlement_bills
    ADD CONSTRAINT chk_sb_special_approval_term
    CHECK (special_approval_ref IS NULL OR special_approval_valid_until IS NOT NULL);

-- ========== ② 期限后台可配(默认 3 个月) ==========
-- 挂在既有「各酒吧分账方式主配置」表上(同 项目 维度、同草案/双人复核转正闸、同前端配置块),
-- 不新建配置表:超池特批本就是该表所配分配方式的溢出出口,同一业务对象同一处配。
ALTER TABLE ticket_distribution_configs
    ADD COLUMN IF NOT EXISTS special_approval_term_months INT NOT NULL DEFAULT 3;

COMMENT ON COLUMN ticket_distribution_configs.special_approval_term_months IS
    '超池特批有效期(月),特批当日 + N 月 = 到期日;出厂默认 3(业务口径:短期激励,默认不超过 3 个月)';

ALTER TABLE ticket_distribution_configs DROP CONSTRAINT IF EXISTS chk_tdc_sa_term;
ALTER TABLE ticket_distribution_configs
    ADD CONSTRAINT chk_tdc_sa_term CHECK (special_approval_term_months >= 1);

-- ========== ③ v7 审计 R-2:超池特批专属权限点(绑平台负责人) ==========
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('settlement.bill.special_approve', '超池特批(平台负责人短期激励·高敏)', 'settlement', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 仅授平台负责人(role_platform_admin),与 settlement.bill.post 分离授权。
INSERT INTO role_permissions (role_id, permission_code)
SELECT v.role_id, v.permission_code
FROM (VALUES
    ('role_platform_admin', 'settlement.bill.special_approve')
) AS v(role_id, permission_code)
ON CONFLICT (role_id, permission_code) DO NOTHING;

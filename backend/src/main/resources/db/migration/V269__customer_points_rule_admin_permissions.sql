-- V269 后台积分规则引擎管理端点权限点（REQ-12, P0-6）。
-- 背景：CustomerPointsRuleRecord + 仓库(save/list/find/listByProject)早已存在，但一直无后台 CRUD 端点
--   来配置积分获取/有效期/过期/等级阈值。本迁移补两个分级权限点并同步授 role_platform_admin（遵循
--   V261 基线：新权限迁移必须同步授平台管理员）。project_id 为综合体级 direct scope，隔离在服务层。
--   ① customer.points_rule.view：查看积分规则（medium）。
--   ② customer.points_rule.manage：维护积分规则（增改，medium 配置写，非资金/成本敏感故非 high）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('customer.points_rule.view', '查看积分规则引擎配置（获取/有效期/过期/等级阈值）', 'customer', 'medium', FALSE, 'active'),
    ('customer.points_rule.manage', '维护积分规则引擎配置（新增/修改积分获取规则、等级阈值、有效期与过期策略）', 'customer', 'medium', FALSE, 'active')
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
        'customer.points_rule.view',
        'customer.points_rule.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- V169 角色装配权限（④ 集团治理 S1）：用户↔角色直接装配，独立权限 access.role.assign。
-- 与 access.user.manage 分离＝防提权：能管用户档案 ≠ 能给人派任意角色（派角色＝授该角色全部权限，
-- 由 AccessService.requireActorCanGrant 再夹一道"不得超出授权人自身权限"）。
-- 角色权限运行时增删沿用既有 access.role.manage。user_roles/role_permissions 表 + 复合主键 V003 已在，无需建表。

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('access.role.assign', 'Assign and unassign roles to users', 'platform', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code FROM permissions
 WHERE permission_code IN ('access.role.assign')
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- BYOD 二期 Wave1（2026-07-19 用户拍板·员工自助/读类）：员工查看+维护本人基础资料的自助权限。
-- 复用既有 staff.payroll.view_mine 承载「工资条自查（含提成明细）」，无需新权限；
-- 本迁移仅新增「本人档案自助」权限，供 GET/POST /api/v1/staff/profiles/mine 使用。
-- 授予口径沿用 V211/V217 员工自助权限约定（授 role_platform_admin），不改动既有 staff 角色装配。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('staff.profile.self', '查看/维护本人基础资料', 'staff', 'low', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'staff.profile.self'
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- V261 平台管理员=全部权限(2026-07-06 用户拍板:admin 是最高权限账号必须有全部权限)。
-- 背景:历史迁移给外包/审计/员工自助等专属角色加权限时未同步授 role_platform_admin,
--   累计漏 25 条(客人账号管理/审计日志/证据导出/核销冲正/外包/员工自助等)。
-- 本迁移一次性补齐,并作为后续基线:新权限迁移必须同步授 role_platform_admin。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', p.permission_code
  FROM permissions p
 WHERE p.status = 'active'
ON CONFLICT (role_id, permission_code) DO NOTHING;

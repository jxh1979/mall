-- V144 游乐设备主数据 CRUD（V143 amusement_devices）权限点 seed。
-- 背景：V143 已建 amusement_devices 表（设备主数据 + 每设备安全检查周期），但有表无管理端点，
-- 运营无法录入/查看设备主数据。本迁移补两个权限点，供新增 CRUD 端点鉴权：
--   1) amusement.device.view   —— 读项目设备主数据明细（只读）。
--   2) amusement.device.manage —— 写入/更新设备主数据，risk 偏高（与 amusement.capacity_config.manage 同级）。
-- seed 写法对齐 V092（amusement.capacity_config.manage）/ V127（amusement.over_capacity_policy.view）约定：
-- view 取 normal/不需审批；manage 取 high/需审批。授予 role_platform_admin（与 capacity_config 既有口径一致）。
-- 向后兼容：仅新增权限点与角色授权，不改动既有 amusement 权限语义。

-- 1) 新增权限点（domain=amusement）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('amusement.device.view', 'View amusement devices', 'amusement', 'normal', FALSE, 'active'),
    ('amusement.device.manage', 'Manage amusement device master data', 'amusement', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

-- 2) 角色授权：授予 platform_admin（与 V092/V127 既有口径一致）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'amusement.device.view',
        'amusement.device.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

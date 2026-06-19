-- V092 第二阶段（运营配置端点 清单#6）：游乐运营配置写权限点 seed。
-- 背景：V082(amusement_project_entitlements) / V083(amusement_capacity_config) 此前有表无管理端点，
-- 运营无法录入「项目↔权益绑定」「点位/设备并发容量」，导致核销绑定校验回退、三维容量第三维不受控。
-- 本迁移补两个高危写权限点，供新增 CRUD 端点鉴权（与 amusement.session.manage 等同级，均为运营配置）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('amusement.capacity_config.manage', 'Manage amusement capacity config', 'amusement', 'high', TRUE, 'active'),
    ('amusement.project_entitlement.manage', 'Manage amusement project entitlement bindings', 'amusement', 'high', TRUE, 'active')
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
        'amusement.capacity_config.manage',
        'amusement.project_entitlement.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

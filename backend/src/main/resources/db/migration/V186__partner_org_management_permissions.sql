-- 条目2 外部协作机构管理权限（2026-06-30）：平台管理员维护机构。平台管理员经 wildcard 桥自动持有，本 seed 供权限目录/矩阵完整。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('partner.org.view',   'View external partner orgs', 'partner', 'low',    FALSE, 'active'),
    ('partner.org.manage', 'Create or update external partner orgs (cap/validity)', 'partner', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, status = EXCLUDED.status;

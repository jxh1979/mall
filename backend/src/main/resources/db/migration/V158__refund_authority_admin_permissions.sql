-- V158 退款分级授权配置管理权限（Stage C，审计 A2 闭环）：平台管理员通过 /api/v1/finance/refund-authority
--   维护员工自助退款额度（refund_authority_limits）+ 自动放行规则（refund_auto_rules），替代手工改库。
--   ① refund.authority.view（medium）查看额度/规则配置；② refund.authority.manage（high）录入/修改（敏感：决定谁能退多少）。
--   纯权限种子（不建表、不改存量数据）；与 V154 酒吧商品管理权限同范式。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('refund.authority.view', 'View refund self-service limits and auto-refund rules', 'finance', 'medium', FALSE, 'active'),
    ('refund.authority.manage', 'Create or update refund self-service limits and auto-refund rules', 'finance', 'high', TRUE, 'active')
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
        'refund.authority.view',
        'refund.authority.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

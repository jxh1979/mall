-- 3.18 商户专区一期:商户自助层最小权限点。
-- 商户自助动作不设 requires_approval(否则商户无法自助);高危平台动作仍走既有平台权限。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('merchant.portal.settlement.view',    'Merchant portal: view own settlements',            'merchant_portal', 'medium', FALSE, 'active'),
    ('merchant.portal.settlement.dispute', 'Merchant portal: dispute own open settlement',     'merchant_portal', 'medium', FALSE, 'active'),
    ('merchant.portal.fee_recipient.manage', 'Merchant portal: manage own fee notification recipients', 'merchant_portal', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 平台管理员默认可监督商户自助层(与其它域 role_platform_admin 全授范式一致)。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'merchant.portal.settlement.view',
        'merchant.portal.settlement.dispute',
        'merchant.portal.fee_recipient.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- 票务订单列表只读权限：补登 GET /api/v1/ticketing/orders 所需的 ticket.order.read。
-- 此前后台票务订单页（adminRoutes ADM-TKT-003）按 ticket.order.read 做前端门禁，
-- 但权限目录缺失该码，后端读取端点也未暴露，导致读取动作落空。

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('ticket.order.read', 'View ticket orders', 'ticketing', 'medium', FALSE, 'active')
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
        'ticket.order.read'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

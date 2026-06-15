INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('amusement.queue.manage', 'Manage amusement queue entries', 'amusement', 'high', TRUE, 'active'),
    ('amusement.reservation.manage', 'Manage amusement reservations', 'amusement', 'high', TRUE, 'active')
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
        'amusement.queue.manage',
        'amusement.reservation.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

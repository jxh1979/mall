INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('governance.supervisor_review.view', 'View supervisor review policies', 'governance', 'medium', FALSE, 'active'),
    ('governance.supervisor_review.manage', 'Evaluate operations against supervisor review policies', 'governance', 'high', TRUE, 'active'),
    ('governance.supervisor_review.approve', 'Approve or reject supervisor review decisions', 'governance', 'critical', TRUE, 'active')
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
        'governance.supervisor_review.view',
        'governance.supervisor_review.manage',
        'governance.supervisor_review.approve'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

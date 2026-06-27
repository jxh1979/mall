-- V173 财务对账差异关闭权限种子。
-- 背景：P0 修改新增 OpenAPI 端点 POST /api/v1/finance/reconciliation/differences/{difference_id}/close
--   (operationId closeFinanceReconciliationDifference)，其 x-permission 引用 finance.reconciliation.resolve，
--   但该权限码此前无种子（只有 finance.reconciliation.difference.create / .view）。本迁移补齐，使契约引用的权限真实存在。
-- 范式：照搬 V105 finance 对账权限 seed；风险等级对齐 integration.reconciliation.resolve（high + requires_approval）。

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.reconciliation.resolve', 'Resolve/close finance reconciliation differences', 'finance', 'high', TRUE, 'active')
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
        'finance.reconciliation.resolve'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

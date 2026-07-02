-- M2 治理口径(V196 范式):经营报表导出与查看分权——看板 analytics.dashboard.view 可看,
-- 导出聚合经营数据须独立权限点 analytics.report.export(高危,不直授审计角色,
-- 审计需要时走 grant-request -> TemporaryGrant 临时授权审批)。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('analytics.report.export', 'Export monthly business report (aggregated business data)', 'analytics', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 平台管理员直授(与 V200/V204 role_platform_admin 范式一致)。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'analytics.report.export'
ON CONFLICT (role_id, permission_code) DO NOTHING;

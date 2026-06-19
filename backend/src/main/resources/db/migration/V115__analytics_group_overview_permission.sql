-- P2 集团/跨项目汇总（R6-D13）：集团总览只读端点 GET /api/v1/analytics/group-overview 的权限。
-- 不新建快照表——集团汇总复用 V037 analytics_business_metric_snapshots 的平台口径 ETL 快照（DashboardAggregationJob 产出），
-- 按 project 维 rollup（当前单综合体即单 project）。本迁移仅补一个细分权限并授予平台管理员。
-- 与 analytics.dashboard.view（看板下钻，可下放至商户）相区分：集团总览跨商户聚合，仅平台/集团管理员可见。

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('analytics.group.view', 'View group/cross-project analytics overview', 'analytics', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code = 'analytics.group.view'
ON CONFLICT (role_id, permission_code) DO NOTHING;

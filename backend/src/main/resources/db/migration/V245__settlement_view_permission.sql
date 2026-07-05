-- 审计断层②(2026-07-05):平台侧新增「结算审阅」只读页,需独立查看权限。
-- 通票核销结算流水此前只落 settlement_transactions + 月度成账 Job,平台操作员无处查看;
-- 本迁移加只读权限 finance.settlement.view(不含确认/入账等写动作),并授予平台管理员角色。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('finance.settlement.view', 'View ticket-pass settlement transactions (read-only review)', 'finance', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 平台管理员直授(wildcard 角色不依赖本行;给显式绑定的管理员角色补上)。
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, 'finance.settlement.view' FROM roles r
 WHERE r.role_name = 'platform_admin'
ON CONFLICT DO NOTHING;

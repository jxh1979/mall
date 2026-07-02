-- 治理口径（业务HTML 2026-06-29）：审计人员查看或导出敏感信息（资金流水等）需先经审批并留痕。
-- V187 已把审计日志导出从审计角色剥离（不直授、走临时授权审批流）；本迁移对资金流水导出做同样收口：
-- 导出从 finance.ar_ap.view 拆出独立权限点 finance.ar_ap.export（高危、不授给审计角色），
-- 内审保留 view（可查流水），导出须经 grant-request -> TemporaryGrant 审批获得临时授权。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('finance.ar_ap.export', 'Export AR/AP ledger snapshot (sensitive money flow)', 'finance', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 平台管理员直授（wildcard 角色不依赖本行；给显式绑定的管理员角色补上）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, 'finance.ar_ap.export' FROM roles r
 WHERE r.role_name = 'platform_admin'
ON CONFLICT DO NOTHING;

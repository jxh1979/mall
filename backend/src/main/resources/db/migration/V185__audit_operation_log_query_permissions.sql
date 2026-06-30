-- 条目4 跨对象操作留痕查询/导出权限（2026-06-30）：审计人员可跨对象搜索/导出 audit_events 全量操作日志。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('audit.operation_log.view',   'Cross-object query of operation audit log (audit_events)', 'audit', 'low',  FALSE, 'active'),
    ('audit.operation_log.export', 'Export full operation audit log across objects (sensitive)', 'audit', 'high', TRUE,  'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;

-- 授给内审/外审两只读角色（export 为敏感，requires_approval=TRUE 在授予层走审批）。
INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_auditor_internal', 'audit.operation_log.view'),
    ('role_auditor_internal', 'audit.operation_log.export'),
    ('role_auditor_external', 'audit.operation_log.view'),
    ('role_auditor_external', 'audit.operation_log.export')
ON CONFLICT (role_id, permission_code) DO NOTHING;

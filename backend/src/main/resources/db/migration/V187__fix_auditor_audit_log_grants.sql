-- 修正 V185 误授（2026-06-30）：审计角色为只读，敏感「导出」走审批不直授；外审角色 role_permissions 保持空（经模板/临时授权）。
-- 保留内审 audit.operation_log.view（跨对象操作留痕只读查询，符合内审 complex 全量只读）。
DELETE FROM role_permissions
 WHERE role_id IN ('role_auditor_internal', 'role_auditor_external')
   AND permission_code = 'audit.operation_log.export';
DELETE FROM role_permissions
 WHERE role_id = 'role_auditor_external'
   AND permission_code = 'audit.operation_log.view';

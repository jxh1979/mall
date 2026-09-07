-- M087-I3-01（PGM-1.10.1「保存所属商户或发生跨项目变更时……新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，
-- 不得因处于岗位模板内而免除授出前审批」）：范围变更申请（user_scope_change）与岗位模板内高危挂起申请（position_high_risk_release）
-- 复用 access_grant_requests；V170 的类型 CHECK 只允许四种既有类型，须扩展，否则写路径在生产库整单回滚。

ALTER TABLE access_grant_requests DROP CONSTRAINT IF EXISTS chk_access_grant_requests_type;

ALTER TABLE access_grant_requests
    ADD CONSTRAINT chk_access_grant_requests_type
    CHECK (request_type IN ('user_role', 'user_position', 'temporary_permission', 'role_permission',
                            'user_scope_change', 'position_high_risk_release'));

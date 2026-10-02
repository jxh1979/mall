-- M002 CODEX-M002-1-01/02：员工单独授予、解除显式拒绝走授权申请；岗位高危挂起与员工显式拒绝分行存放（PGM 权限规则「显式拒绝优先级最高」「授出前须先审批才生效」）
-- 存量挂起行不回填，一律按 explicit 处理（AGENTS.md 第 8 条）：迁移前开出、迁移后才获批的释放单删 0 行，该码继续被拒绝。
ALTER TABLE access_grant_requests DROP CONSTRAINT IF EXISTS chk_access_grant_requests_type;
ALTER TABLE access_grant_requests ADD CONSTRAINT chk_access_grant_requests_type
    CHECK (request_type IN ('user_role', 'user_position', 'temporary_permission', 'role_permission',
                            'user_scope_change', 'position_high_risk_release',
                            'user_permission_grant', 'user_permission_deny_removal'));
ALTER TABLE user_permission_overrides ADD COLUMN origin VARCHAR(16) NOT NULL DEFAULT 'explicit';
ALTER TABLE user_permission_overrides ADD CONSTRAINT chk_user_permission_overrides_origin
    CHECK (origin = 'explicit' OR (origin = 'position_hold' AND effect = 'deny'));
ALTER TABLE user_permission_overrides
    DROP CONSTRAINT user_permission_overrides_pkey,
    ADD PRIMARY KEY (user_id, permission_code, effect, origin);

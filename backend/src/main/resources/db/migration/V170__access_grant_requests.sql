-- V170 高危授权审批（④ 集团治理 S2）：激活休眠的 permissions.requires_approval。
-- 当 access.grant_approval.enabled 开 且 目标权限 requires_approval=true：直接装配被拒，改走授权申请流——
-- 建 pending 申请并经既有主管复核(SupervisorReview，policy permission_grant_l3，hardcoded)生成待批；
-- 审批通过(过 SoD/职级/角色闸)才落 user_roles/role_permissions。本表承载申请意图 + 关联 review_id + 状态机。
-- 复用既有审批引擎，不另造；policy 在 SupervisorReviewService 代码内（该域 policy 为 in-code 清单），故无 policy 种子。

CREATE TABLE IF NOT EXISTS access_grant_requests (
    id VARCHAR(64) PRIMARY KEY,
    request_type VARCHAR(32) NOT NULL,             -- user_role | role_permission
    target_user_id VARCHAR(64),                    -- user_role 时必填
    target_role_id VARCHAR(64) NOT NULL,           -- 两类申请都涉及角色
    permission_code VARCHAR(128),                  -- role_permission 时必填
    status VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending|applied|rejected
    review_id VARCHAR(64),                          -- 关联 supervisor_review_decisions.review_id
    reason VARCHAR(1000),
    requested_by VARCHAR(64),
    decided_by VARCHAR(64),
    decided_at TIMESTAMPTZ,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_agr_type CHECK (request_type IN ('user_role', 'role_permission')),
    CONSTRAINT chk_agr_status CHECK (status IN ('pending', 'applied', 'rejected'))
);
CREATE INDEX IF NOT EXISTS idx_agr_status ON access_grant_requests (status, created_at DESC);

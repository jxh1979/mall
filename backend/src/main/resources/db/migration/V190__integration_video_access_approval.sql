-- 2.2 视频调阅强制复核：新增主管审批表；调阅须凭 approved 审批（对象点位+查看人匹配、未过期）。
-- 现状 integration_video_access_audits.approval_ref 只是自由文本留痕、无校验；本迁移把它变成可校验的审批引用。
CREATE TABLE IF NOT EXISTS integration_video_access_approvals (
    id VARCHAR(64) PRIMARY KEY,               -- = approval_ref，调阅时引用
    viewer_user_id VARCHAR(64) NOT NULL,      -- 被授权调阅的员工
    point_id VARCHAR(64),                     -- 授权对象点位；NULL=全部点位
    status VARCHAR(32) NOT NULL,              -- pending/approved/rejected
    approved_by VARCHAR(64),                  -- 复核主管
    valid_until TIMESTAMPTZ,                  -- 有效期；NULL=不限
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_video_access_approval_status CHECK (status IN ('pending', 'approved', 'rejected'))
);

CREATE INDEX IF NOT EXISTS idx_integration_video_access_approvals_viewer
    ON integration_video_access_approvals (viewer_user_id, status);

-- 主管复核授权视频调阅的权限点（高危）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('integration.video.approve', 'Approve video access review', 'integration', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

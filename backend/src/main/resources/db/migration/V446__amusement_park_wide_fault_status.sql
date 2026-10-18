-- V446 CG-08a 全园故障状态（ENUM「全园故障与补偿通票」）：
--   「全园故障状态由游乐公司负责人和工程或安全岗位共同确认后生效，一经生效，必须立即停止所有纯游乐门票和所有包含游乐权益的
--    通票的新销售；全园故障状态的解除同样由游乐公司负责人和工程或安全岗位共同确认，解除后恢复上述票种的新销售」
-- 按综合体项目一条状态记录：发起 → 待第二岗位确认 → 生效；解除同样两步。两次确认须不同人、且分别持两个岗位对应权限。全程留痕。

CREATE TABLE IF NOT EXISTS amusement_park_wide_faults (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects (id),  -- 综合体项目
    status VARCHAR(32) NOT NULL,                        -- pending_activation | active | pending_lift | lifted
    reason TEXT NOT NULL,                               -- 故障情形（全部项目不可运行 / 供电、消防、入口系统等公共关键设施故障）
    initiated_by VARCHAR(64) NOT NULL,                  -- 发起人（第一岗位）
    initiated_role VARCHAR(32) NOT NULL,                -- amusement_head（游乐公司负责人）| engineering_safety（工程或安全岗位）
    initiated_at TIMESTAMPTZ NOT NULL,
    activation_confirmed_by VARCHAR(64),                -- 第二岗位确认人（与发起人不同人、不同岗位）
    activation_confirmed_role VARCHAR(32),
    activated_at TIMESTAMPTZ,                           -- 生效时点：自此停售纯游乐票与含游乐权益通票
    lift_initiated_by VARCHAR(64),
    lift_initiated_role VARCHAR(32),
    lift_initiated_at TIMESTAMPTZ,
    lift_reason TEXT,
    lift_confirmed_by VARCHAR(64),
    lift_confirmed_role VARCHAR(32),
    lifted_at TIMESTAMPTZ,                              -- 解除时点：自此恢复上述票种新销售
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_amusement_park_wide_faults_status
        CHECK (status IN ('pending_activation', 'active', 'pending_lift', 'lifted')),
    CONSTRAINT chk_amusement_park_wide_faults_initiated_role
        CHECK (initiated_role IN ('amusement_head', 'engineering_safety'))
);

-- 同一综合体同一时刻只允许一条未解除的全园故障状态。
CREATE UNIQUE INDEX IF NOT EXISTS uq_amusement_park_wide_faults_open
    ON amusement_park_wide_faults (project_id) WHERE status <> 'lifted';
CREATE INDEX IF NOT EXISTS idx_amusement_park_wide_faults_project_created
    ON amusement_park_wide_faults (project_id, created_at DESC);

-- 留痕：每一次发起 / 确认 / 解除发起 / 解除确认各一行（谁、以哪个岗位、何时、说明）。
CREATE TABLE IF NOT EXISTS amusement_park_wide_fault_actions (
    id VARCHAR(64) PRIMARY KEY,
    fault_id VARCHAR(64) NOT NULL REFERENCES amusement_park_wide_faults (id),
    project_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,                        -- initiate | confirm_activation | initiate_lift | confirm_lift
    actor_id VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_amusement_park_wide_fault_actions_action
        CHECK (action IN ('initiate', 'confirm_activation', 'initiate_lift', 'confirm_lift'))
);
CREATE INDEX IF NOT EXISTS idx_amusement_park_wide_fault_actions_fault
    ON amusement_park_wide_fault_actions (fault_id, created_at);

-- 两个岗位对应的权限点（技术决定：岗位 → 权限码映射）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('amusement.park_fault.view', '查看全园故障状态与留痕', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.park_fault.confirm_as_head', '以游乐公司负责人身份发起/确认全园故障状态生效与解除(高敏)', 'amusement', 'high', TRUE, 'active'),
    ('amusement.park_fault.confirm_as_engineering_safety', '以工程或安全岗位身份发起/确认全园故障状态生效与解除(高敏)', 'amusement', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT v.role_id, v.permission_code
FROM (VALUES
    ('role_platform_admin', 'amusement.park_fault.view'),
    ('role_platform_admin', 'amusement.park_fault.confirm_as_head'),
    ('role_platform_admin', 'amusement.park_fault.confirm_as_engineering_safety')
) AS v(role_id, permission_code)
WHERE EXISTS (SELECT 1 FROM roles r WHERE r.id = v.role_id)
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- 生产受限运行时权限（V298/V299 教训：新表必须显式 GRANT 给 complex_app）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON amusement_park_wide_faults TO complex_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON amusement_park_wide_fault_actions TO complex_app;
    END IF;
END $$;

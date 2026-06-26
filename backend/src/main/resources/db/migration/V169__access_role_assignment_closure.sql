-- 集团治理 S1：角色装配闭环。
-- 既有 RBAC 表（roles/role_permissions/user_roles/permissions）保持不动；
-- 本迁移只补职位/层级映射、员工职位归属和独立角色装配权限。

CREATE TABLE IF NOT EXISTS position_role_templates (
    id VARCHAR(64) PRIMARY KEY,
    position_code VARCHAR(64) NOT NULL,
    staff_level_id VARCHAR(64) NOT NULL REFERENCES staff_levels(id),
    role_id VARCHAR(64) NOT NULL REFERENCES roles(id),
    template_id VARCHAR(64) NOT NULL REFERENCES permission_templates(id),
    organization_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_position_role_templates_status CHECK (status IN ('active','inactive'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_position_role_templates_active_scope
    ON position_role_templates (COALESCE(organization_id, ''), position_code, staff_level_id)
 WHERE status = 'active';

CREATE INDEX IF NOT EXISTS idx_position_role_templates_scope
    ON position_role_templates (organization_id, position_code, staff_level_id);

CREATE TABLE IF NOT EXISTS user_position_assignments (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES access_users(id) ON DELETE CASCADE,
    position_code VARCHAR(64) NOT NULL,
    staff_level_id VARCHAR(64) NOT NULL REFERENCES staff_levels(id),
    organization_id VARCHAR(64),
    merchant_id VARCHAR(64),
    assigned_by VARCHAR(64),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_position_assignments_org
    ON user_position_assignments (organization_id)
 WHERE organization_id IS NOT NULL;

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('access.role.assign', 'Assign and unassign roles to users', 'access', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'access.role.assign'
 WHERE EXISTS (SELECT 1 FROM roles WHERE id = 'role_platform_admin')
ON CONFLICT (role_id, permission_code) DO NOTHING;

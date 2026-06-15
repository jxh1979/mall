CREATE TABLE IF NOT EXISTS permissions (
    permission_code VARCHAR(128) PRIMARY KEY,
    permission_name VARCHAR(128) NOT NULL,
    domain VARCHAR(64) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS access_users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    account_type VARCHAR(32) NOT NULL,
    organization_id VARCHAR(64),
    merchant_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(64) PRIMARY KEY,
    role_name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    data_scope VARCHAR(64) NOT NULL DEFAULT 'project',
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id VARCHAR(64) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_code VARCHAR(128) NOT NULL REFERENCES permissions(permission_code),
    PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(64) NOT NULL REFERENCES access_users(id) ON DELETE CASCADE,
    role_id VARCHAR(64) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

ALTER TABLE permission_templates
    ADD COLUMN IF NOT EXISTS template_code VARCHAR(128),
    ADD COLUMN IF NOT EXISTS template_type VARCHAR(32) NOT NULL DEFAULT 'base',
    ADD COLUMN IF NOT EXISTS source_template_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS owner_scope_type VARCHAR(64),
    ADD COLUMN IF NOT EXISTS owner_scope_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS usage_mode VARCHAR(64) NOT NULL DEFAULT 'merge',
    ADD COLUMN IF NOT EXISTS valid_from TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS valid_to TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS single_use BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'active',
    ADD COLUMN IF NOT EXISTS reason TEXT,
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(128),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE permission_templates
   SET template_code = id
 WHERE template_code IS NULL;

UPDATE permission_templates
   SET updated_at = created_at
 WHERE updated_at IS NULL;

ALTER TABLE permission_templates
    ALTER COLUMN template_code SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_permission_templates_template_code
    ON permission_templates (template_code);

CREATE TABLE IF NOT EXISTS permission_template_items (
    template_id VARCHAR(64) NOT NULL REFERENCES permission_templates(id) ON DELETE CASCADE,
    permission_code VARCHAR(128) NOT NULL REFERENCES permissions(permission_code),
    effect VARCHAR(16) NOT NULL,
    data_scope VARCHAR(64) NOT NULL DEFAULT 'project',
    sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (template_id, permission_code, effect),
    CONSTRAINT chk_permission_template_items_effect CHECK (effect IN ('grant', 'deny'))
);

INSERT INTO permission_template_items (template_id, permission_code, effect, data_scope, sensitive)
SELECT template_id, permission_code, 'grant', 'project', FALSE
  FROM permission_template_permissions
ON CONFLICT (template_id, permission_code, effect) DO NOTHING;

CREATE TABLE IF NOT EXISTS permission_template_applications (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL REFERENCES permission_templates(id) ON DELETE CASCADE,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    single_use BOOLEAN NOT NULL DEFAULT FALSE,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    consumed_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    applied_by VARCHAR(128),
    applied_at TIMESTAMPTZ NOT NULL,
    revoked_by VARCHAR(128),
    revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_permission_template_applications_target
    ON permission_template_applications (target_type, target_id, status);

CREATE TABLE IF NOT EXISTS permission_template_usage_logs (
    id UUID PRIMARY KEY,
    application_id VARCHAR(64) REFERENCES permission_template_applications(id) ON DELETE SET NULL,
    user_id VARCHAR(64),
    permission_code VARCHAR(128),
    result VARCHAR(32) NOT NULL,
    reason VARCHAR(128),
    occurred_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE temporary_permission_grants
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'active',
    ADD COLUMN IF NOT EXISTS valid_from TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoked_by VARCHAR(128),
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS single_use BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS consumed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS consumed_at TIMESTAMPTZ;

UPDATE temporary_permission_grants
   SET valid_from = granted_at
 WHERE valid_from IS NULL;

ALTER TABLE temporary_permission_grants
    ALTER COLUMN valid_from SET NOT NULL;

CREATE TABLE IF NOT EXISTS user_permission_overrides (
    user_id VARCHAR(64) NOT NULL REFERENCES access_users(id) ON DELETE CASCADE,
    permission_code VARCHAR(128) NOT NULL REFERENCES permissions(permission_code),
    effect VARCHAR(16) NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(128),
    PRIMARY KEY (user_id, permission_code, effect),
    CONSTRAINT chk_user_permission_overrides_effect CHECK (effect IN ('grant', 'deny'))
);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('access.permission.view', 'View permission dictionary', 'access', 'medium', FALSE, 'active'),
    ('access.permission_template.view', 'View permission templates', 'access', 'medium', FALSE, 'active'),
    ('access.permission_template.manage', 'Manage permission templates', 'access', 'high', TRUE, 'active'),
    ('access.permission_template.apply', 'Apply permission templates', 'access', 'high', TRUE, 'active'),
    ('access.permission_template.revoke', 'Revoke permission template applications', 'access', 'high', TRUE, 'active'),
    ('access.role.view', 'View roles', 'access', 'medium', FALSE, 'active'),
    ('access.role.manage', 'Manage roles', 'access', 'high', TRUE, 'active'),
    ('access.temporary_grant.create', 'Create temporary grants', 'access', 'high', TRUE, 'active'),
    ('access.temporary_grant.revoke', 'Revoke temporary grants', 'access', 'high', TRUE, 'active'),
    ('access.user.view', 'View access users', 'access', 'medium', FALSE, 'active'),
    ('access.user.manage', 'Manage access users', 'access', 'high', TRUE, 'active'),
    ('ops.production_readiness.view', 'View production readiness', 'ops', 'medium', FALSE, 'active'),
    ('ops.production_readiness.manage', 'Manage production readiness', 'ops', 'high', TRUE, 'active'),
    ('ops.production_readiness.check', 'Run production readiness checks', 'ops', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO roles (id, role_name, description, data_scope, status, created_at)
VALUES ('role_platform_admin', 'platform_admin', 'P0 platform administrator role', 'global', 'active', now())
ON CONFLICT (id) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    data_scope = EXCLUDED.data_scope,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE status = 'active'
ON CONFLICT (role_id, permission_code) DO NOTHING;

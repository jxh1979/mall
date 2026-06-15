CREATE TABLE IF NOT EXISTS business_entities (
    id VARCHAR(64) PRIMARY KEY,
    entity_name VARCHAR(120) NOT NULL,
    entity_role VARCHAR(32) NOT NULL,
    unified_social_credit_code VARCHAR(128),
    legal_representative VARCHAR(255),
    bank_account_ref TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_business_entities_status
    ON business_entities (status);

CREATE TABLE IF NOT EXISTS merchants (
    id VARCHAR(64) PRIMARY KEY,
    merchant_name VARCHAR(120) NOT NULL,
    merchant_type VARCHAR(32) NOT NULL,
    business_entity_id VARCHAR(64),
    contract_entity_id VARCHAR(64),
    settlement_entity_id VARCHAR(64),
    contact_name VARCHAR(120),
    contact_phone VARCHAR(20),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_merchants_business_entity
    ON merchants (business_entity_id);

CREATE INDEX IF NOT EXISTS idx_merchants_status
    ON merchants (status);

CREATE TABLE IF NOT EXISTS projects (
    id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64),
    venue_type VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(64),
    floor VARCHAR(32),
    capacity INTEGER,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_projects_merchant
    ON projects (merchant_id);

CREATE INDEX IF NOT EXISTS idx_projects_status
    ON projects (status);

CREATE TABLE IF NOT EXISTS spaces (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    parent_space_id VARCHAR(64),
    space_name VARCHAR(120) NOT NULL,
    space_type VARCHAR(64) NOT NULL,
    floor_no VARCHAR(128),
    area_square_meter NUMERIC(18,2),
    polygon_points JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_spaces_project
    ON spaces (project_id);

CREATE INDEX IF NOT EXISTS idx_spaces_merchant
    ON spaces (merchant_id);

CREATE INDEX IF NOT EXISTS idx_spaces_parent
    ON spaces (parent_space_id);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('entity.binding.view', 'View business entities and bindings', 'platform', 'medium', FALSE, 'active'),
    ('entity.binding.manage', 'Manage business entities and bindings', 'platform', 'high', TRUE, 'active'),
    ('merchant.profile.view', 'View merchant profiles', 'platform', 'medium', FALSE, 'active'),
    ('merchant.profile.manage', 'Manage merchant profiles', 'platform', 'high', TRUE, 'active'),
    ('governance.project.view', 'View platform projects', 'governance', 'medium', FALSE, 'active'),
    ('governance.project.manage', 'Manage platform projects', 'governance', 'high', TRUE, 'active'),
    ('governance.space.view', 'View platform spaces', 'governance', 'medium', FALSE, 'active'),
    ('governance.space.manage', 'Manage platform spaces', 'governance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'entity.binding.view',
        'entity.binding.manage',
        'merchant.profile.view',
        'merchant.profile.manage',
        'governance.project.view',
        'governance.project.manage',
        'governance.space.view',
        'governance.space.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

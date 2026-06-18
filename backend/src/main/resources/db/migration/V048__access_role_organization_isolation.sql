-- Add organization_id to roles table for tenant isolation (P0 security fix).
-- NULL = platform-global role visible to all; non-null = organization-scoped role.
ALTER TABLE roles ADD COLUMN IF NOT EXISTS organization_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_roles_organization
    ON roles (organization_id)
 WHERE organization_id IS NOT NULL;

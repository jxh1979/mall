CREATE TABLE IF NOT EXISTS mall_floor_plans (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    building_code VARCHAR(64),
    floor_no VARCHAR(64) NOT NULL,
    plan_name VARCHAR(160) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    image_width INTEGER NOT NULL,
    image_height INTEGER NOT NULL,
    version_no VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_mall_floor_plans_image_size CHECK (image_width > 0 AND image_height > 0),
    CONSTRAINT chk_mall_floor_plans_status CHECK (status IN ('active', 'inactive', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_mall_floor_plans_project_floor
    ON mall_floor_plans (project_id, building_code, floor_no, status);

CREATE TABLE IF NOT EXISTS floor_plan_shapes (
    id VARCHAR(64) PRIMARY KEY,
    floor_plan_id VARCHAR(64) NOT NULL REFERENCES mall_floor_plans(id) ON DELETE CASCADE,
    shape_type VARCHAR(32) NOT NULL,
    ref_type VARCHAR(32) NOT NULL,
    ref_id VARCHAR(64),
    label VARCHAR(160) NOT NULL,
    polygon_points JSONB NOT NULL,
    z_index INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_floor_plan_shapes_shape_type CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route')),
    CONSTRAINT chk_floor_plan_shapes_ref_type CHECK (ref_type IN ('space', 'merchant', 'project', 'amusement_project', 'device', 'custom')),
    CONSTRAINT chk_floor_plan_shapes_status CHECK (status IN ('active', 'inactive', 'archived')),
    CONSTRAINT chk_floor_plan_shapes_points_array CHECK (jsonb_typeof(polygon_points) = 'array' AND jsonb_array_length(polygon_points) >= 3)
);

CREATE INDEX IF NOT EXISTS idx_floor_plan_shapes_plan
    ON floor_plan_shapes (floor_plan_id, status, z_index);

CREATE INDEX IF NOT EXISTS idx_floor_plan_shapes_ref
    ON floor_plan_shapes (ref_type, ref_id)
 WHERE ref_id IS NOT NULL;

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('floor_plan.view', 'View floor plans and polygon shapes', 'floor_plan', 'medium', FALSE, 'active'),
    ('floor_plan.manage', 'Manage floor plans and polygon shapes', 'floor_plan', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('floor_plan.view', 'floor_plan.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;

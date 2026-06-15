CREATE TABLE IF NOT EXISTS floor_plan_import_tasks (
    id VARCHAR(64) PRIMARY KEY,
    floor_plan_id VARCHAR(64) NOT NULL REFERENCES mall_floor_plans(id) ON DELETE CASCADE,
    source_format VARCHAR(32) NOT NULL,
    source_name VARCHAR(160) NOT NULL,
    source_hash VARCHAR(128) NOT NULL,
    source_payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    candidate_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    operator_note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_floor_plan_import_tasks_source_format CHECK (source_format IN ('svg', 'standard_json')),
    CONSTRAINT chk_floor_plan_import_tasks_status CHECK (status IN ('parsed', 'failed', 'published', 'cancelled')),
    CONSTRAINT chk_floor_plan_import_tasks_counts CHECK (candidate_count >= 0 AND error_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_floor_plan_import_tasks_plan
    ON floor_plan_import_tasks (floor_plan_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS floor_plan_import_candidates (
    id VARCHAR(64) PRIMARY KEY,
    import_task_id VARCHAR(64) NOT NULL REFERENCES floor_plan_import_tasks(id) ON DELETE CASCADE,
    candidate_no INTEGER NOT NULL,
    shape_type VARCHAR(32) NOT NULL,
    ref_type VARCHAR(32) NOT NULL,
    ref_id VARCHAR(64),
    label VARCHAR(160) NOT NULL,
    polygon_points JSONB NOT NULL,
    z_index INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_floor_plan_import_candidates_no CHECK (candidate_no > 0),
    CONSTRAINT chk_floor_plan_import_candidates_shape_type CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route')),
    CONSTRAINT chk_floor_plan_import_candidates_ref_type CHECK (ref_type IN ('space', 'merchant', 'project', 'amusement_project', 'device', 'custom')),
    CONSTRAINT chk_floor_plan_import_candidates_status CHECK (status IN ('pending', 'published', 'rejected')),
    CONSTRAINT chk_floor_plan_import_candidates_points_array CHECK (jsonb_typeof(polygon_points) = 'array' AND jsonb_array_length(polygon_points) >= 3)
);

CREATE INDEX IF NOT EXISTS idx_floor_plan_import_candidates_task
    ON floor_plan_import_candidates (import_task_id, candidate_no, status);

CREATE TABLE IF NOT EXISTS floor_plan_import_errors (
    id VARCHAR(64) PRIMARY KEY,
    import_task_id VARCHAR(64) NOT NULL REFERENCES floor_plan_import_tasks(id) ON DELETE CASCADE,
    line_no INTEGER,
    field_name VARCHAR(120),
    error_code VARCHAR(80) NOT NULL,
    error_message VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_floor_plan_import_errors_task
    ON floor_plan_import_errors (import_task_id, id);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('floor_plan.import.view', 'View floor plan import tasks and candidates', 'floor_plan', 'medium', FALSE, 'active'),
    ('floor_plan.import.manage', 'Manage floor plan imports and publish candidates', 'floor_plan', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('floor_plan.import.view', 'floor_plan.import.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;

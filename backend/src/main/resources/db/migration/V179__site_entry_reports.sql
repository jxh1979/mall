-- B-construction: construction/renovation site entry reports.

CREATE TABLE IF NOT EXISTS site_entry_reports (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    space_id VARCHAR(64),
    work_scope TEXT NOT NULL,
    planned_start_date DATE NOT NULL,
    planned_end_date DATE NOT NULL,
    crew_count INTEGER NOT NULL,
    contact_name VARCHAR(128) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    vendor_user_id VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'submitted',
    review_comment TEXT,
    acceptance_comment TEXT,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_ser_status CHECK (status IN ('submitted','approved','rejected','in_progress','accepted','rectifying')),
    CONSTRAINT chk_ser_dates CHECK (planned_end_date >= planned_start_date),
    CONSTRAINT chk_ser_crew_count CHECK (crew_count > 0)
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_name = 'site_entry_reports' AND column_name = 'planned_start'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_name = 'site_entry_reports' AND column_name = 'planned_start_date'
    ) THEN
        ALTER TABLE site_entry_reports RENAME COLUMN planned_start TO planned_start_date;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_name = 'site_entry_reports' AND column_name = 'planned_end'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_name = 'site_entry_reports' AND column_name = 'planned_end_date'
    ) THEN
        ALTER TABLE site_entry_reports RENAME COLUMN planned_end TO planned_end_date;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_name = 'site_entry_reports' AND column_name = 'assignee_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_name = 'site_entry_reports' AND column_name = 'vendor_user_id'
    ) THEN
        ALTER TABLE site_entry_reports RENAME COLUMN assignee_id TO vendor_user_id;
    END IF;
END $$;

ALTER TABLE site_entry_reports
    ADD COLUMN IF NOT EXISTS planned_start_date DATE,
    ADD COLUMN IF NOT EXISTS planned_end_date DATE,
    ADD COLUMN IF NOT EXISTS vendor_user_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE site_entry_reports
    DROP CONSTRAINT IF EXISTS chk_ser_status,
    DROP CONSTRAINT IF EXISTS chk_ser_dates,
    DROP CONSTRAINT IF EXISTS chk_ser_crew_count,
    ADD CONSTRAINT chk_ser_status CHECK (status IN ('submitted','approved','rejected','in_progress','accepted','rectifying')),
    ADD CONSTRAINT chk_ser_dates CHECK (planned_end_date >= planned_start_date),
    ADD CONSTRAINT chk_ser_crew_count CHECK (crew_count > 0);

ALTER TABLE site_entry_reports
    ALTER COLUMN planned_start_date SET NOT NULL,
    ALTER COLUMN planned_end_date SET NOT NULL,
    ALTER COLUMN vendor_user_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ser_vendor_project
    ON site_entry_reports (vendor_user_id, project_id)
 WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_ser_project_status
    ON site_entry_reports (project_id, status)
 WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_ser_space
    ON site_entry_reports (space_id)
 WHERE space_id IS NOT NULL AND deleted_at IS NULL;

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('facility.site_entry.view_assigned', 'View assigned site entry reports', 'facility', 'medium', FALSE, 'active'),
    ('facility.site_entry.create_assigned', 'Create assigned site entry reports', 'facility', 'medium', FALSE, 'active'),
    ('facility.site_entry.submit_assigned', 'Submit assigned site entry reports', 'facility', 'medium', FALSE, 'active'),
    ('facility.site_entry.review', 'Review site entry reports', 'facility', 'medium', FALSE, 'active'),
    ('facility.site_entry.accept', 'Accept site entry reports', 'facility', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO roles (id, role_name, description, data_scope, status, created_at)
VALUES
    ('role_outsourced_construction', 'OUTSOURCED_CONSTRUCTION',
     'Outsourced construction and renovation role limited to own site entry reports',
     'assignee', 'active', now()),
    ('role_ops_manager', 'OPS_MANAGER',
     'Operations manager role for project facility operations',
     'project', 'active', now()),
    ('role_facility_manager', 'FACILITY_MANAGER',
     'Facility manager role for project facility operations',
     'project', 'active', now())
ON CONFLICT (id) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    data_scope = EXCLUDED.data_scope,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_outsourced_construction', 'facility.site_entry.view_assigned'),
    ('role_outsourced_construction', 'facility.site_entry.create_assigned'),
    ('role_outsourced_construction', 'facility.site_entry.submit_assigned')
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO permission_templates (
    id, template_name, description, template_code, template_type, owner_scope_type,
    usage_mode, status, created_at, updated_at
)
VALUES
    ('tpl_outsourced_construction', 'OUTSOURCED_CONSTRUCTION',
     'Assigned-only site entry report template for outsourced construction vendors',
     'OUTSOURCED_CONSTRUCTION', 'role', 'platform', 'merge', 'active', now(), now())
ON CONFLICT (id) DO UPDATE SET
    template_name = EXCLUDED.template_name,
    description = EXCLUDED.description,
    template_code = EXCLUDED.template_code,
    template_type = EXCLUDED.template_type,
    owner_scope_type = EXCLUDED.owner_scope_type,
    usage_mode = EXCLUDED.usage_mode,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

INSERT INTO permission_template_items (template_id, permission_code, effect, data_scope, sensitive)
VALUES
    ('tpl_outsourced_construction', 'facility.site_entry.view_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_construction', 'facility.site_entry.create_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_construction', 'facility.site_entry.submit_assigned', 'grant', 'assignee', FALSE)
ON CONFLICT (template_id, permission_code, effect) DO NOTHING;

INSERT INTO position_role_templates (
    id, position_code, staff_level_id, role_id, template_id, organization_id, status, created_by, created_at, updated_at
)
VALUES
    ('prt_outsourced_construction', 'outsourced_construction', 'lvl_m1_lower',
     'role_outsourced_construction', 'tpl_outsourced_construction', NULL, 'active', 'V179', now(), now())
ON CONFLICT (id) DO UPDATE SET
    position_code = EXCLUDED.position_code,
    staff_level_id = EXCLUDED.staff_level_id,
    role_id = EXCLUDED.role_id,
    template_id = EXCLUDED.template_id,
    organization_id = EXCLUDED.organization_id,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, p.permission_code
  FROM roles r
 CROSS JOIN (VALUES
        ('facility.site_entry.review'),
        ('facility.site_entry.accept')
       ) AS p(permission_code)
 WHERE r.id IN ('role_platform_admin', 'role_ops_manager', 'role_facility_manager')
    OR r.role_name IN ('platform_admin', 'OPS_MANAGER', 'FACILITY_MANAGER', 'ops_manager', 'facility_manager')
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- B-core: outsourced vendor facility assignee-only permissions.

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('facility.work_order.view_assigned', 'View assigned facility work orders', 'facility', 'medium', FALSE, 'active'),
    ('facility.work_order.process_assigned', 'Process assigned facility work orders', 'facility', 'medium', FALSE, 'active'),
    ('facility.work_order.accept_assigned', 'Accept assigned facility work orders', 'facility', 'medium', FALSE, 'active'),
    ('facility.inspection.view_assigned', 'View assigned facility inspection tasks', 'facility', 'medium', FALSE, 'active'),
    ('facility.inspection.record_assigned', 'Record assigned facility inspection tasks', 'facility', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO roles (id, role_name, description, data_scope, status, created_at)
VALUES
    ('role_outsourced_facility', 'OUTSOURCED_FACILITY',
     'Outsourced facility vendor role limited to assigned work orders', 'assignee', 'active', now()),
    ('role_outsourced_security_cleaning', 'OUTSOURCED_SECURITY_CLEANING',
     'Outsourced security and cleaning role limited to assigned inspection tasks', 'assignee', 'active', now())
ON CONFLICT (id) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    data_scope = EXCLUDED.data_scope,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_outsourced_facility', 'facility.work_order.view_assigned'),
    ('role_outsourced_facility', 'facility.work_order.process_assigned'),
    ('role_outsourced_facility', 'facility.work_order.accept_assigned'),
    ('role_outsourced_security_cleaning', 'facility.inspection.view_assigned'),
    ('role_outsourced_security_cleaning', 'facility.inspection.record_assigned')
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO permission_templates (
    id, template_name, description, template_code, template_type, owner_scope_type,
    usage_mode, status, created_at, updated_at
)
VALUES
    ('tpl_outsourced_facility', 'OUTSOURCED_FACILITY',
     'Assigned-only facility work order template for outsourced facility vendors',
     'OUTSOURCED_FACILITY', 'role', 'platform', 'merge', 'active', now(), now()),
    ('tpl_outsourced_security_cleaning', 'OUTSOURCED_SECURITY_CLEANING',
     'Assigned-only inspection task template for outsourced security and cleaning vendors',
     'OUTSOURCED_SECURITY_CLEANING', 'role', 'platform', 'merge', 'active', now(), now())
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
    ('tpl_outsourced_facility', 'facility.work_order.view_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_facility', 'facility.work_order.process_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_facility', 'facility.work_order.accept_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_security_cleaning', 'facility.inspection.view_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_security_cleaning', 'facility.inspection.record_assigned', 'grant', 'assignee', FALSE)
ON CONFLICT (template_id, permission_code, effect) DO NOTHING;

INSERT INTO position_role_templates (
    id, position_code, staff_level_id, role_id, template_id, organization_id, status, created_by, created_at, updated_at
)
VALUES
    ('prt_outsourced_facility', 'outsourced_facility', 'lvl_m1_lower',
     'role_outsourced_facility', 'tpl_outsourced_facility', NULL, 'active', 'V178', now(), now()),
    ('prt_outsourced_security_cleaning', 'outsourced_security_cleaning', 'lvl_m1_lower',
     'role_outsourced_security_cleaning', 'tpl_outsourced_security_cleaning', NULL, 'active', 'V178', now(), now())
ON CONFLICT (id) DO UPDATE SET
    position_code = EXCLUDED.position_code,
    staff_level_id = EXCLUDED.staff_level_id,
    role_id = EXCLUDED.role_id,
    template_id = EXCLUDED.template_id,
    organization_id = EXCLUDED.organization_id,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

CREATE INDEX IF NOT EXISTS idx_fwo_assignee_project
    ON facility_work_orders (assignee_id, project_id)
 WHERE assignee_id IS NOT NULL AND project_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_fitask_assignee_project
    ON facility_inspection_tasks (assignee_id, project_id)
 WHERE assignee_id IS NOT NULL AND project_id IS NOT NULL;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM (VALUES
        ('facility.work_order.view_assigned'),
        ('facility.work_order.process_assigned'),
        ('facility.work_order.accept_assigned'),
        ('facility.inspection.view_assigned'),
        ('facility.inspection.record_assigned')
       ) AS p(permission_code)
 WHERE EXISTS (SELECT 1 FROM roles WHERE id = 'role_platform_admin')
ON CONFLICT (role_id, permission_code) DO NOTHING;

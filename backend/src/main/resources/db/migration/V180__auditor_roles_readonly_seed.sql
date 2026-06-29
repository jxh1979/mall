-- C-audit grading: auditor roles/templates are read-only seed data.
-- Runtime approval is still the existing grant-request -> TemporaryGrant flow.
-- This migration intentionally does not update permissions.requires_approval.

INSERT INTO roles (id, role_name, description, data_scope, status, created_at)
VALUES
    ('role_auditor_internal', 'AUDITOR_INTERNAL',
     'Internal auditor role with complex-scoped read-only access',
     'complex', 'active', now()),
    ('role_auditor_external', 'AUDITOR_EXTERNAL',
     'External auditor role with project-scoped read-only access; time-box grants with TemporaryGrant.validUntil',
     'project', 'active', now())
ON CONFLICT (id) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    data_scope = EXCLUDED.data_scope,
    status = EXCLUDED.status;

INSERT INTO permission_templates (
    id, template_name, description, template_code, template_type, owner_scope_type,
    usage_mode, valid_from, valid_to, single_use, status, created_at, updated_at
)
VALUES
    ('tpl_auditor_internal', 'AUDITOR_INTERNAL',
     'Complex-scoped read-only permission template for internal auditors',
     'AUDITOR_INTERNAL', 'role', 'platform', 'merge', NULL, NULL, FALSE, 'active', now(), now()),
    ('tpl_auditor_external', 'AUDITOR_EXTERNAL',
     'Project-scoped read-only permission template for external auditors; grant actual access with TemporaryGrant.validUntil',
     'AUDITOR_EXTERNAL', 'role', 'platform', 'merge', NULL, NULL, FALSE, 'active', now(), now())
ON CONFLICT (id) DO UPDATE SET
    template_name = EXCLUDED.template_name,
    description = EXCLUDED.description,
    template_code = EXCLUDED.template_code,
    template_type = EXCLUDED.template_type,
    owner_scope_type = EXCLUDED.owner_scope_type,
    usage_mode = EXCLUDED.usage_mode,
    valid_from = EXCLUDED.valid_from,
    valid_to = EXCLUDED.valid_to,
    single_use = EXCLUDED.single_use,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

WITH read_only_permissions AS (
    SELECT permission_code, domain
      FROM permissions
     WHERE status = 'active'
       AND permission_code ~ '\.(view|read)$'
       AND permission_code !~ '\.(manage|approve|export|create|submit|review|accept|process|resolve|refund|adjust|assign)$'
),
external_project_permissions AS (
    SELECT permission_code
      FROM read_only_permissions
     WHERE domain IN (
            'amusement',
            'analytics',
            'bar',
            'compliance',
            'facility',
            'finance',
            'floor_plan',
            'inventory',
            'merchant_billing',
            'merchant_service',
            'ticketing',
            'verification'
       )
       AND permission_code <> 'analytics.group.view'
)
DELETE FROM role_permissions rp
 WHERE rp.role_id = 'role_auditor_internal'
   AND rp.permission_code NOT IN (SELECT permission_code FROM read_only_permissions);

WITH read_only_permissions AS (
    SELECT permission_code, domain
      FROM permissions
     WHERE status = 'active'
       AND permission_code ~ '\.(view|read)$'
       AND permission_code !~ '\.(manage|approve|export|create|submit|review|accept|process|resolve|refund|adjust|assign)$'
),
external_project_permissions AS (
    SELECT permission_code
      FROM read_only_permissions
     WHERE domain IN (
            'amusement',
            'analytics',
            'bar',
            'compliance',
            'facility',
            'finance',
            'floor_plan',
            'inventory',
            'merchant_billing',
            'merchant_service',
            'ticketing',
            'verification'
       )
       AND permission_code <> 'analytics.group.view'
)
DELETE FROM role_permissions rp
 WHERE rp.role_id = 'role_auditor_external'
   AND rp.permission_code NOT IN (SELECT permission_code FROM external_project_permissions);

WITH read_only_permissions AS (
    SELECT permission_code
      FROM permissions
     WHERE status = 'active'
       AND permission_code ~ '\.(view|read)$'
       AND permission_code !~ '\.(manage|approve|export|create|submit|review|accept|process|resolve|refund|adjust|assign)$'
)
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_auditor_internal', permission_code
  FROM read_only_permissions
ON CONFLICT (role_id, permission_code) DO NOTHING;

WITH read_only_permissions AS (
    SELECT permission_code, domain
      FROM permissions
     WHERE status = 'active'
       AND permission_code ~ '\.(view|read)$'
       AND permission_code !~ '\.(manage|approve|export|create|submit|review|accept|process|resolve|refund|adjust|assign)$'
),
external_project_permissions AS (
    SELECT permission_code
      FROM read_only_permissions
     WHERE domain IN (
            'amusement',
            'analytics',
            'bar',
            'compliance',
            'facility',
            'finance',
            'floor_plan',
            'inventory',
            'merchant_billing',
            'merchant_service',
            'ticketing',
            'verification'
       )
       AND permission_code <> 'analytics.group.view'
)
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_auditor_external', permission_code
  FROM external_project_permissions
ON CONFLICT (role_id, permission_code) DO NOTHING;

WITH read_only_permissions AS (
    SELECT permission_code, domain
      FROM permissions
     WHERE status = 'active'
       AND permission_code ~ '\.(view|read)$'
       AND permission_code !~ '\.(manage|approve|export|create|submit|review|accept|process|resolve|refund|adjust|assign)$'
),
external_project_permissions AS (
    SELECT permission_code
      FROM read_only_permissions
     WHERE domain IN (
            'amusement',
            'analytics',
            'bar',
            'compliance',
            'facility',
            'finance',
            'floor_plan',
            'inventory',
            'merchant_billing',
            'merchant_service',
            'ticketing',
            'verification'
       )
       AND permission_code <> 'analytics.group.view'
)
DELETE FROM permission_template_items pti
 WHERE pti.template_id = 'tpl_auditor_internal'
   AND pti.permission_code NOT IN (SELECT permission_code FROM read_only_permissions);

WITH read_only_permissions AS (
    SELECT permission_code, domain
      FROM permissions
     WHERE status = 'active'
       AND permission_code ~ '\.(view|read)$'
       AND permission_code !~ '\.(manage|approve|export|create|submit|review|accept|process|resolve|refund|adjust|assign)$'
),
external_project_permissions AS (
    SELECT permission_code
      FROM read_only_permissions
     WHERE domain IN (
            'amusement',
            'analytics',
            'bar',
            'compliance',
            'facility',
            'finance',
            'floor_plan',
            'inventory',
            'merchant_billing',
            'merchant_service',
            'ticketing',
            'verification'
       )
       AND permission_code <> 'analytics.group.view'
)
DELETE FROM permission_template_items pti
 WHERE pti.template_id = 'tpl_auditor_external'
   AND pti.permission_code NOT IN (SELECT permission_code FROM external_project_permissions);

WITH read_only_permissions AS (
    SELECT permission_code
      FROM permissions
     WHERE status = 'active'
       AND permission_code ~ '\.(view|read)$'
       AND permission_code !~ '\.(manage|approve|export|create|submit|review|accept|process|resolve|refund|adjust|assign)$'
)
INSERT INTO permission_template_items (template_id, permission_code, effect, data_scope, sensitive)
SELECT 'tpl_auditor_internal', permission_code, 'grant', 'complex', FALSE
  FROM read_only_permissions
ON CONFLICT (template_id, permission_code, effect) DO UPDATE SET
    data_scope = EXCLUDED.data_scope,
    sensitive = EXCLUDED.sensitive;

WITH read_only_permissions AS (
    SELECT permission_code, domain
      FROM permissions
     WHERE status = 'active'
       AND permission_code ~ '\.(view|read)$'
       AND permission_code !~ '\.(manage|approve|export|create|submit|review|accept|process|resolve|refund|adjust|assign)$'
),
external_project_permissions AS (
    SELECT permission_code
      FROM read_only_permissions
     WHERE domain IN (
            'amusement',
            'analytics',
            'bar',
            'compliance',
            'facility',
            'finance',
            'floor_plan',
            'inventory',
            'merchant_billing',
            'merchant_service',
            'ticketing',
            'verification'
       )
       AND permission_code <> 'analytics.group.view'
)
INSERT INTO permission_template_items (template_id, permission_code, effect, data_scope, sensitive)
SELECT 'tpl_auditor_external', permission_code, 'grant', 'project', FALSE
  FROM external_project_permissions
ON CONFLICT (template_id, permission_code, effect) DO UPDATE SET
    data_scope = EXCLUDED.data_scope,
    sensitive = EXCLUDED.sensitive;

INSERT INTO position_role_templates (
    id, position_code, staff_level_id, role_id, template_id, organization_id, status, created_by, created_at, updated_at
)
VALUES
    ('prt_auditor_internal', 'auditor_internal', 'lvl_m1_lower',
     'role_auditor_internal', 'tpl_auditor_internal', NULL, 'active', 'V180', now(), now()),
    ('prt_auditor_external', 'auditor_external', 'lvl_m1_lower',
     'role_auditor_external', 'tpl_auditor_external', NULL, 'active', 'V180', now(), now())
ON CONFLICT (id) DO UPDATE SET
    position_code = EXCLUDED.position_code,
    staff_level_id = EXCLUDED.staff_level_id,
    role_id = EXCLUDED.role_id,
    template_id = EXCLUDED.template_id,
    organization_id = EXCLUDED.organization_id,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

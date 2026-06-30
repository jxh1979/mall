-- B-adjust + C-lock: outsourced vendors and external auditors use time-boxed TemporaryGrant access.
-- user_roles has no valid_until, so direct runtime role grants for these groups are removed.

ALTER TABLE site_entry_reports
    ADD COLUMN IF NOT EXISTS completion_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS completion_note TEXT,
    ADD COLUMN IF NOT EXISTS completion_uploaded_at TIMESTAMPTZ;

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('facility.equipment.view', 'View project facility equipment registry', 'facility', 'medium', FALSE, 'active'),
    ('facility.inspection.template.view', 'View facility inspection templates', 'facility', 'medium', FALSE, 'active'),
    ('facility.inspection.report_exception_assigned', 'Report exceptions for assigned facility inspection tasks', 'facility', 'medium', FALSE, 'active'),
    ('facility.site_entry.upload_completion_assigned', 'Upload completion materials for assigned site entry reports', 'facility', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

UPDATE roles
   SET description = CASE id
        WHEN 'role_outsourced_facility'
            THEN 'Outsourced facility vendor role; grant runtime permissions with TemporaryGrant.validUntil'
        WHEN 'role_outsourced_security_cleaning'
            THEN 'Outsourced security and cleaning role; grant runtime permissions with TemporaryGrant.validUntil'
        WHEN 'role_outsourced_construction'
            THEN 'Outsourced construction and renovation role; grant runtime permissions with TemporaryGrant.validUntil'
        WHEN 'role_auditor_external'
            THEN 'External auditor role; project read-only permissions must be granted with TemporaryGrant.validUntil'
        ELSE description
       END
 WHERE id IN (
        'role_outsourced_facility',
        'role_outsourced_security_cleaning',
        'role_outsourced_construction',
        'role_auditor_external'
 );

DELETE FROM role_permissions
 WHERE role_id IN (
        'role_outsourced_facility',
        'role_outsourced_security_cleaning',
        'role_outsourced_construction',
        'role_auditor_external'
 );

DELETE FROM permission_template_items
 WHERE template_id = 'tpl_outsourced_facility'
   AND permission_code = 'facility.work_order.accept_assigned';

INSERT INTO permission_template_items (template_id, permission_code, effect, data_scope, sensitive)
VALUES
    ('tpl_outsourced_facility', 'facility.work_order.view_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_facility', 'facility.work_order.process_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_facility', 'facility.equipment.view', 'grant', 'project', FALSE),
    ('tpl_outsourced_security_cleaning', 'facility.inspection.view_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_security_cleaning', 'facility.inspection.record_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_security_cleaning', 'facility.inspection.template.view', 'grant', 'project', FALSE),
    ('tpl_outsourced_security_cleaning', 'facility.inspection.report_exception_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_construction', 'facility.site_entry.view_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_construction', 'facility.site_entry.create_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_construction', 'facility.site_entry.submit_assigned', 'grant', 'assignee', FALSE),
    ('tpl_outsourced_construction', 'facility.site_entry.upload_completion_assigned', 'grant', 'assignee', FALSE)
ON CONFLICT (template_id, permission_code, effect) DO UPDATE SET
    data_scope = EXCLUDED.data_scope,
    sensitive = EXCLUDED.sensitive;

INSERT INTO role_permissions (role_id, permission_code)
SELECT role_id, permission_code
  FROM (VALUES
        ('role_platform_admin', 'facility.work_order.manage'),
        ('role_platform_admin', 'facility.equipment.view'),
        ('role_platform_admin', 'facility.inspection.template.view'),
        ('role_platform_admin', 'facility.inspection.report_exception_assigned'),
        ('role_platform_admin', 'facility.site_entry.upload_completion_assigned'),
        ('role_ops_manager', 'facility.work_order.manage'),
        ('role_ops_manager', 'facility.equipment.view'),
        ('role_ops_manager', 'facility.inspection.template.view'),
        ('role_facility_manager', 'facility.work_order.manage'),
        ('role_facility_manager', 'facility.equipment.view'),
        ('role_facility_manager', 'facility.inspection.template.view')
       ) AS p(role_id, permission_code)
 WHERE EXISTS (SELECT 1 FROM roles r WHERE r.id = p.role_id)
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_auditor_internal', 'facility.equipment.view'),
    ('role_auditor_internal', 'facility.inspection.template.view')
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO permission_template_items (template_id, permission_code, effect, data_scope, sensitive)
VALUES
    ('tpl_auditor_internal', 'facility.equipment.view', 'grant', 'complex', FALSE),
    ('tpl_auditor_internal', 'facility.inspection.template.view', 'grant', 'complex', FALSE),
    ('tpl_auditor_external', 'facility.equipment.view', 'grant', 'project', FALSE),
    ('tpl_auditor_external', 'facility.inspection.template.view', 'grant', 'project', FALSE)
ON CONFLICT (template_id, permission_code, effect) DO UPDATE SET
    data_scope = EXCLUDED.data_scope,
    sensitive = EXCLUDED.sensitive;

-- R3 修复 P0-4/P0-5 端点暴露：职级/薪资接口的权限点种子。
-- 配合 GET /api/v1/access/staff-levels（access.staff_level.view）
-- 与 PUT /api/v1/access/users/{id}/staff-level（access.staff_level.manage，高风险，需审批）。
-- 独立于 V055（V055 已应用，不可改），避免 Flyway checksum 失配。

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('access.staff_level.view', 'View staff levels', 'access', 'medium', FALSE, 'active'),
    ('access.staff_level.manage', 'Assign staff level and salary', 'access', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- BYOD 员工自助角色补链(2026-07-19,既有缺口修复,非 Wave1 引入)
-- 缺口:StaffService.reviewRegistration 审批通过只 createUser(userType=staff, role=null)+发凭证,
--       从不为新员工分配任何角色;而自助权限(staff.payroll.view_mine / staff.shift.view /
--       staff.attendance.punch / staff.leave.submit / staff.notice.view / staff.profile.self)
--       仅授给 role_platform_admin(V211/V217/V321)。权限解析(JdbcAccessRepository.findPermissionEffects)
--       只读 user_roles→roles→role_permissions,故真实审批通过的员工(userType=staff)无任何 user_roles 行
--       → 所有 BYOD 自助端点对其一律 403;此前仅 platform_admin 桥可用(E/F 批测试即以 admin token 跑,掩盖了此缺口)。
-- 本迁移:定义"员工自助"角色 role_staff_self 并授予上述自助权限;审批通过分支据此为新员工分配该角色。
-- data_scope='self' 为文档性标注:自助端点按 actor.id() 自锁到本人,商户/项目隔离由
--       access_users.merchant_id / organization_id 驱动(AuthorizationService 不读 role.data_scope)。
-- 说明:staff.profile.self 权限本身由 V321(BYOD Wave1，已在 main、号更小必先跑)定义，此处直接授予即可。

INSERT INTO roles (id, role_name, description, data_scope, status, created_at)
VALUES ('role_staff_self', 'STAFF_SELF',
        'BYOD staff self-service role: view own payroll/shifts/attendance/notices, punch, submit leave',
        'self', 'active', now())
ON CONFLICT (id) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    data_scope = EXCLUDED.data_scope,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_staff_self', p.code FROM (VALUES
    ('staff.payroll.view_mine'),
    ('staff.shift.view'),
    ('staff.attendance.punch'),
    ('staff.leave.submit'),
    ('staff.notice.view'),
    ('staff.profile.self')
) AS p(code)
ON CONFLICT (role_id, permission_code) DO NOTHING;

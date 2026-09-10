-- 文件版本：V0.01
-- M010-001、M010-020：姓名加密长度；本人绑定权限，主体归属由服务校验。
ALTER TABLE staff_profiles ALTER COLUMN name TYPE TEXT;
INSERT INTO role_permissions(role_id, permission_code)
VALUES ('role_staff_self', 'staff.device.bind')
ON CONFLICT DO NOTHING;

-- BYOD 二期 E 批(2026-07-02 拍板):主管排班(双端:后台页+主管App,班次到分钟+指定点位)、
-- NFC/扫码考勤打卡(严格无宽限:晚于班次开始即迟到、早于结束即早退;只记录不自动扣罚)。
CREATE TABLE IF NOT EXISTS staff_shift_schedules (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    work_date DATE NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    point_space_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    note VARCHAR(255),
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_staff_shift UNIQUE (user_id, start_at),
    CONSTRAINT chk_staff_shift_range CHECK (end_at > start_at)
);
CREATE INDEX IF NOT EXISTS idx_staff_shift_date ON staff_shift_schedules (work_date, user_id);

CREATE TABLE IF NOT EXISTS staff_attendance_records (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    punch_type VARCHAR(16) NOT NULL,
    punched_at TIMESTAMPTZ NOT NULL,
    method VARCHAR(16) NOT NULL,
    tag_ref VARCHAR(200),
    space_id VARCHAR(64),
    device_fingerprint VARCHAR(128),
    shift_id VARCHAR(64),
    judgement VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_att_type CHECK (punch_type IN ('check_in', 'check_out')),
    CONSTRAINT chk_att_method CHECK (method IN ('nfc', 'qr', 'manual')),
    CONSTRAINT chk_att_judgement CHECK (judgement IN ('normal', 'late', 'early_leave', 'no_shift'))
);
CREATE INDEX IF NOT EXISTS idx_staff_att_user_time ON staff_attendance_records (user_id, punched_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('staff.shift.manage',     'Manage staff shift schedules (supervisor)', 'staff', 'medium', FALSE, 'active'),
    ('staff.shift.view',       'View own shift schedule',                   'staff', 'low',    FALSE, 'active'),
    ('staff.attendance.punch', 'Punch attendance (check in/out)',           'staff', 'low',    FALSE, 'active'),
    ('staff.attendance.view',  'View attendance records (supervisor)',     'staff', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', p.code FROM (VALUES
    ('staff.shift.manage'), ('staff.shift.view'), ('staff.attendance.punch'), ('staff.attendance.view')) AS p(code)
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- 文件版本：V0.01
-- HP-H06/H12：记录真实顶班时段及手机考勤证据。
ALTER TABLE staff_shift_covers
    ADD COLUMN start_at TIMESTAMPTZ NOT NULL,
    ADD COLUMN end_at TIMESTAMPTZ NOT NULL,
    ADD COLUMN handover_note TEXT NOT NULL,
    ADD COLUMN cover_shift_id VARCHAR(64) NOT NULL REFERENCES staff_shift_schedules(id),
    ADD COLUMN source_updated_at TIMESTAMPTZ NOT NULL,
    ADD CONSTRAINT chk_staff_cover_period CHECK (end_at > start_at),
    ADD CONSTRAINT uq_staff_cover_effective_shift UNIQUE (cover_shift_id);

ALTER TABLE staff_attendance_records DROP CONSTRAINT chk_att_method;
ALTER TABLE staff_attendance_records ADD CONSTRAINT chk_att_method
    CHECK (method IN ('nfc','qr','manual','charging_disconnect','device_return'));

CREATE TABLE staff_work_phone_contexts (
    id VARCHAR(64) PRIMARY KEY,
    device_id VARCHAR(128) NOT NULL,
    binding_at TIMESTAMPTZ NOT NULL,
    staff_profile_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    shift_id VARCHAR(64) REFERENCES staff_shift_schedules(id),
    previous_context_id VARCHAR(64) REFERENCES staff_work_phone_contexts(id),
    closed_at TIMESTAMPTZ,
    close_reason VARCHAR(24),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_work_phone_context_binding UNIQUE (device_id,binding_at),
    CONSTRAINT chk_work_phone_context_close CHECK (close_reason IS NULL OR close_reason IN ('shift_end','fault_swap'))
);
CREATE INDEX idx_work_phone_context_user ON staff_work_phone_contexts(user_id,shift_id);

CREATE TABLE staff_attendance_time_anchors (
    id VARCHAR(64) PRIMARY KEY,
    context_id VARCHAR(64) NOT NULL REFERENCES staff_work_phone_contexts(id),
    boot_id VARCHAR(128) NOT NULL,
    elapsed_millis BIGINT NOT NULL CHECK (elapsed_millis >= 0),
    server_at TIMESTAMPTZ NOT NULL,
    issued_by VARCHAR(64) NOT NULL
);

CREATE TABLE staff_work_phone_events (
    id VARCHAR(64) PRIMARY KEY,
    submitted_by VARCHAR(64) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    context_id VARCHAR(64) REFERENCES staff_work_phone_contexts(id),
    anchor_id VARCHAR(64),
    boot_id VARCHAR(128),
    elapsed_millis BIGINT,
    observed_at TIMESTAMPTZ,
    method VARCHAR(24) NOT NULL CHECK (method IN ('nfc','qr','charging_disconnect')),
    tag_ref VARCHAR(200),
    space_id VARCHAR(64),
    payload JSONB NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    shift_id VARCHAR(64),
    actual_at TIMESTAMPTZ,
    status VARCHAR(24) NOT NULL CHECK (status IN ('pending_review','accepted','rejected')),
    reason VARCHAR(128),
    attendance_id VARCHAR(64),
    review_id VARCHAR(64),
    review_payload JSONB,
    received_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_work_phone_accepted_time CHECK (status <> 'accepted' OR (actual_at IS NOT NULL AND attendance_id IS NOT NULL))
);
CREATE INDEX idx_work_phone_events_user ON staff_work_phone_events(user_id,received_at);
CREATE INDEX idx_work_phone_events_review ON staff_work_phone_events(status,review_id);

CREATE TABLE staff_attendance_time_corrections (
    id VARCHAR(64) PRIMARY KEY,
    attendance_id VARCHAR(64) NOT NULL,
    event_id VARCHAR(64) NOT NULL UNIQUE REFERENCES staff_work_phone_events(id),
    before_at TIMESTAMPTZ NOT NULL,
    after_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (after_at < before_at)
);

-- 仅供既有岗位治理装配；不向平台管理员或通用设备管理角色普发。
INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status)
VALUES('terminal.work_phone.swap','夜班值班经理办理工作手机故障换机','terminal','high',TRUE,'active')
ON CONFLICT (permission_code) DO NOTHING;


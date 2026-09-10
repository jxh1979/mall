-- 文件版本：V0.01
CREATE TABLE staff_locker_records (
    id VARCHAR(80) PRIMARY KEY,
    staff_profile_id VARCHAR(80) NOT NULL REFERENCES staff_profiles(id),
    cabinet_device_id VARCHAR(128) NOT NULL,
    slot_ref VARCHAR(128) NOT NULL,
    stored_at TIMESTAMPTZ NOT NULL,
    retrieved_at TIMESTAMPTZ,
    CHECK (retrieved_at IS NULL OR retrieved_at >= stored_at)
);
CREATE UNIQUE INDEX staff_locker_open_slot ON staff_locker_records(cabinet_device_id,slot_ref)
    WHERE retrieved_at IS NULL;
CREATE INDEX staff_locker_staff ON staff_locker_records(staff_profile_id,stored_at);

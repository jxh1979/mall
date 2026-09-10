-- 文件版本：V0.01
-- U05-006（PGM L582 / BSM L938「由柜子自动记录，不设手工登记」）：员工在本人手机会话下申请的短时扫码凭证；
-- 柜机以已登记 kiosk 设备令牌单端上报格口事件时，服务端凭该凭证换回员工身份。一次性、到期作废。
CREATE TABLE staff_locker_scan_tokens (
    id VARCHAR(64) PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    staff_profile_id VARCHAR(64) NOT NULL REFERENCES staff_profiles(id),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    used_by_device_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_staff_locker_scan_tokens_profile ON staff_locker_scan_tokens(staff_profile_id);

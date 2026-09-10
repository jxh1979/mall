-- 文件版本：V0.01
-- 业务任职状态独立于账号及注册审批状态。
ALTER TABLE staff_profiles ADD COLUMN employment_status VARCHAR(32) NOT NULL DEFAULT 'pending_entry'
    CHECK (employment_status IN ('pending_entry', 'employed', 'trial', 'suspended', 'departed'));

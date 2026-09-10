-- 文件版本：V0.02
ALTER TABLE staff_profiles ADD COLUMN company_id VARCHAR(64);
ALTER TABLE staff_profiles ADD COLUMN registration_source VARCHAR(32)
    CHECK (registration_source IN ('merchant_submitted', 'company_self'));
CREATE INDEX idx_staff_profiles_company ON staff_profiles(company_id) WHERE delete_status<>'approved';

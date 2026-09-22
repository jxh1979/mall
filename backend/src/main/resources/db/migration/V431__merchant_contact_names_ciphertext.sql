-- 文件版本：V0.01
-- M009-059：主次联系人姓名的加密信封需要容纳原姓名长度及加密开销。
ALTER TABLE merchants
    ALTER COLUMN contact_name TYPE TEXT,
    ALTER COLUMN secondary_contact_name TYPE TEXT;

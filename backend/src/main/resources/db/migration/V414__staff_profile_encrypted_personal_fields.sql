-- 文件版本：V0.01
-- M010-019：接收可解密原值；不尝试从旧哈希恢复资料。
ALTER TABLE staff_profiles ADD COLUMN phone TEXT;
ALTER TABLE staff_profiles ADD COLUMN id_doc TEXT;

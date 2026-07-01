-- 客人手机号 PII 加密（方案A：AES-256-GCM 密文落库 + HMAC 盲索引查找）。
-- 存量明文由启动期回填（CustomerPhonePiiBackfill）加密并计算盲索引；此处仅动 schema。
--   1) 新增盲索引列 phone_hash（先可空，供存量行回填；新写入由仓储直接置值）。
--   2) 扩宽 phone 以容纳 enc:v1 信封密文（约 60 字符，留足到 128）。
--   3) 唯一约束从明文 phone 迁到盲索引 phone_hash（NULL 视为互异，回填前不误撞）。
ALTER TABLE customers ADD COLUMN IF NOT EXISTS phone_hash CHAR(64);
ALTER TABLE customers ALTER COLUMN phone TYPE VARCHAR(128);
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_phone_key;
ALTER TABLE customers ADD CONSTRAINT customers_phone_hash_key UNIQUE (phone_hash);
CREATE INDEX IF NOT EXISTS idx_customers_phone_hash ON customers (phone_hash);

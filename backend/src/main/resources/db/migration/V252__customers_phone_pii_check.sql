-- customers.phone 落库加密 DB 级边界(2026-07-06,闭合 V249 留档:存量测试直插明文已改造为密文)。
-- 明文写入在数据库层直接失败,任何写路径都绕不过;信封 enc:v1:;NULL/空放行(与 V249 两列口径一致)。
ALTER TABLE customers
  ADD CONSTRAINT chk_customers_phone_pii
  CHECK (phone IS NULL OR phone = '' OR phone LIKE 'enc:v1:%');

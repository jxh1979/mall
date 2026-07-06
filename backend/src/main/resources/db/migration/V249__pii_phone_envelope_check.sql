-- 消费者 PII 落库加密 DB 级边界(2026-07-05,codex 复核建议:从源码扫描升级为数据库强制)。
-- 明文写入在数据库层直接失败,任何写路径(仓库/手写SQL/批量导入)都绕不过。信封 enc:v1:;NULL/空放行。
-- 覆盖走仓库加密、无测试直插明文的两列;customers.phone 因存量测试直插明文,待改造后另迁移补(留档)。
ALTER TABLE ticket_orders
  ADD CONSTRAINT chk_ticket_orders_buyer_phone_pii
  CHECK (buyer_phone IS NULL OR buyer_phone = '' OR buyer_phone LIKE 'enc:v1:%');
ALTER TABLE amusement_group_bookings
  ADD CONSTRAINT chk_amusement_contact_phone_pii
  CHECK (contact_phone IS NULL OR contact_phone = '' OR contact_phone LIKE 'enc:v1:%');

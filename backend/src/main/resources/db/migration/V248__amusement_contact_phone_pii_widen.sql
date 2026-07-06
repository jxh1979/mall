-- 游乐团体预订联系人手机号 PII 加密(审计 F-P2):contact_phone 由明文改 AES-GCM 密文(信封 enc:v1:base64),
-- 原 VARCHAR(64) 容不下密文,加宽到 128(与 customers.phone/ticket_orders.buyer_phone 一致)。存量启动期回填加密。
ALTER TABLE amusement_group_bookings ALTER COLUMN contact_phone TYPE VARCHAR(128);

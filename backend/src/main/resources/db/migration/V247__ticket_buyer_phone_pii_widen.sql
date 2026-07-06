-- 购票人手机号 PII 加密(审计 F-P1):buyer_phone 由明文改为 AES-GCM 密文落库(信封 enc:v1:base64...),
-- 原 VARCHAR(20) 容不下密文,加宽到 128(与 customers.phone/V188 一致)。存量明文由启动期回填就地加密。
ALTER TABLE ticket_orders ALTER COLUMN buyer_phone TYPE VARCHAR(128);

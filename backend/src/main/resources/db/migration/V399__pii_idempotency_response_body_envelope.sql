-- V399 审计运行 20260902T183912Z · XM-013（操作员 D-029 单独立项）：幂等重放响应体加密存放。
--
-- 治理地图 PGM-1.15.2「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放」。
-- idempotency_records.response_body 此前把服务层返回对象原样序列化落库（jsonb）：票务订单 buyer_phone、团体预订 contact_phone、
-- 客人档案 phone 等在业务表里是 enc:v1: 密文，在这张表里却是明文；表无过期清理，明文长期留存。
--   ① 列类型 jsonb → text：改为存放 PiiCipher 的 enc:v1: 信封串（AES-256-GCM），读回解密后再反序列化；
--   ② 加 NOT VALID 的 CHECK：存量明文行放行（启动期由 IdempotencyResponseBodyPiiBackfill 就地加密，幂等），
--      任何新写入 / 更新必须是信封密文（与 V339 多主体联系人同一范式）。
ALTER TABLE idempotency_records ALTER COLUMN response_body TYPE TEXT USING response_body::text;

ALTER TABLE idempotency_records DROP CONSTRAINT IF EXISTS chk_idempotency_records_response_body_pii;
ALTER TABLE idempotency_records
    ADD CONSTRAINT chk_idempotency_records_response_body_pii
    CHECK (response_body IS NULL OR response_body = '' OR response_body LIKE 'enc:v1:%') NOT VALID;

-- V398 审计运行 20260902T183912Z · M009 · 操作员裁定 R-005（2026-09-07）：柜台允许无会员散客拍脸照，同意留痕落到订单。
--
-- BSM-1.7.3「采集证件、人脸、支付账号、精确位置等敏感信息前先单独取得同意，并留同意版本、时间和范围」。
-- 客人端 H5 的人脸照同意挂在会员账号（customer_consents.customer_id）。柜台出票的散客没有会员账号，按 R-005 其同意
-- 以订单为主体落账（购票人手机号由订单 buyer_phone 承载），仍用同一张同意台账、同一 consent_type='ticket_face_photo'，
-- 不新建第二套台账（V368 原则）。
--
-- 改动：customer_id 放开为可空；新增 order_id；CHECK 保证每条同意至少有一个主体（会员或订单）。
-- 既有会员同意行不受影响（customer_id 非空）。不对 order_id 加外键：订单归档/清理不应被同意行阻断，同意行本身永久留痕。
ALTER TABLE customer_consents ALTER COLUMN customer_id DROP NOT NULL;
ALTER TABLE customer_consents ADD COLUMN IF NOT EXISTS order_id VARCHAR(64);
ALTER TABLE customer_consents DROP CONSTRAINT IF EXISTS chk_customer_consents_subject;
ALTER TABLE customer_consents
    ADD CONSTRAINT chk_customer_consents_subject
    CHECK (customer_id IS NOT NULL OR order_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_customer_consents_order ON customer_consents (order_id);

COMMENT ON COLUMN customer_consents.order_id IS
    'R-005 柜台散客人脸照同意的主体订单(ticket_orders.id)：customer_id 为空时必填；同意只对该订单有效(出票链比对 consent.order_id = 订单)';

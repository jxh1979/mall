-- 文件版本：V0.01
-- V525 TK-06 一人多次票首次核销留脸（BSM:780「购票人或被赠予人核销第一张起留存人脸，后续各次只允许同一人使用并以该人脸核对」；
-- BSM:843 / ENUM:60 / PGM:891「一人多次票在首次核销时留存人脸……该人脸在该票用完或到期后删除；多人票与单次票不留人脸、不做票脸绑定。
-- 不设需要人脸照的票种；购票和转赠均不要求录入人脸照」；BSM:844 原始人脸图像取最短留存期限；台账 A-01）。
-- 1) 购票 / 出票 / 柜台采的人脸照与订单暂存位用途已撤销：全部删除（最短留存），撤掉「需要人脸照」开关与「票失效后再留 N 天」配置。
-- 2) ticket_face_photos 改为一张父票（一人多次票）一条绑定：首次核销时拍的人脸，挂当次同意与首次核销记录。
--    删除时点由清理任务按「父票下已无可用入场权益（用完或到期）」实时判定，不再存固定的 purge_after。
DELETE FROM ticket_face_photos;

ALTER TABLE ticket_face_photos DROP CONSTRAINT IF EXISTS chk_tfp_owner_present;
ALTER TABLE ticket_face_photos DROP CONSTRAINT IF EXISTS uq_tfp_order_slot;
DROP INDEX IF EXISTS idx_tfp_purge_after;
ALTER TABLE ticket_face_photos
    DROP COLUMN IF EXISTS entitlement_id,
    DROP COLUMN IF EXISTS order_id,
    DROP COLUMN IF EXISTS slot_index,
    DROP COLUMN IF EXISTS purge_after;

ALTER TABLE ticket_face_photos
    ADD COLUMN ticket_instance_id VARCHAR(64) NOT NULL,
    ADD COLUMN first_verification_id VARCHAR(64) NOT NULL;
ALTER TABLE ticket_face_photos
    ADD CONSTRAINT uq_tfp_ticket_instance UNIQUE (ticket_instance_id);
ALTER TABLE ticket_face_photos
    ADD CONSTRAINT fk_tfp_ticket_instance FOREIGN KEY (ticket_instance_id) REFERENCES ticket_instances(id);

COMMENT ON COLUMN ticket_face_photos.ticket_instance_id IS
    'TK-06 绑定的父票实例（一人多次票）；一张票一条，首次核销时留存，用完或到期后由清理任务删除';
COMMENT ON COLUMN ticket_face_photos.first_verification_id IS
    'TK-06 留存人脸的那次核销记录 id（ticket_verifications.id，仅追溯）';
COMMENT ON COLUMN ticket_face_photos.consent_id IS
    'TK-06 首次核销留脸前单独取得的同意（customer_consents.id，consent_type=multi_use_ticket_face）';

ALTER TABLE entitlement_templates DROP COLUMN IF EXISTS require_face_photo;
ALTER TABLE issued_entitlements DROP COLUMN IF EXISTS require_face_photo;

DELETE FROM notification_windows WHERE window_key = 'ticketing.face_photo_retention';

-- V395 审计运行 20260902T183912Z · M009 A1-01：人脸照同意留痕对所有存档行生效。
--
-- 业务地图 BSM-1.7.3「采集证件、人脸、支付账号、精确位置等敏感信息前先单独取得同意，并留同意版本、时间和范围」没有入口例外。
-- V368 只约束客人端暂存位（order_id 非空）：柜台/后台出票请求自带的照片落权益位时 consent_id 恒空；
-- 客人端已同意的照片在出票转绑到权益位时同意 id 也被丢掉（暂存行随后删除）——出票之后没有任何一张存档脸照
-- 能指回「哪一版告知、何时同意」。
--
-- 代码侧同批修复：权益位落库带 consent_id；转绑沿用暂存位的同意；出票请求自带照片必须带已生效的同意留痕 id
-- （FACE_PHOTO_CONSENT_REQUIRED / FACE_PHOTO_CONSENT_INVALID）。库上把 CHECK 收紧到「任何一行都必须有同意」，
-- 任何代码路径（含将来新写的）都绕不过去。
--
-- 前置：两库 ticket_face_photos 现无任何行（2026-09-07 于 160 complex / complex_test 实查），无需回填；
-- 若目标库存在 consent_id 为空的历史行，本迁移会失败——那正是「有照没同意」的合规事故，须人工裁定处置后再上。
ALTER TABLE ticket_face_photos DROP CONSTRAINT IF EXISTS chk_tfp_consent_present;
ALTER TABLE ticket_face_photos
    ADD CONSTRAINT chk_tfp_consent_present
    CHECK (consent_id IS NOT NULL);

COMMENT ON COLUMN ticket_face_photos.consent_id IS
    '这张脸照对应的同意留痕 id(customer_consents.id);客人端暂存位与出票权益位一律必填(chk_tfp_consent_present,V395),出票转绑沿用暂存位的同意';

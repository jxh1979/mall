-- V229 结算重构批3-6(用户2026-07-04新增需求):游乐票购买人脸拍照存档。
--   口径:游乐票购买时做人脸校验、拍一张照片加密存档,核销时后台显示这张照片,核销员确认核销的是本人(防转票)。
--   与现有 entry/ 人脸入场闸(判 18+ 年龄闸·fail-closed·不存照)是两回事——本能力=票面人脸照绑定+核销显示,独立。
--   适用范围:全票种**可配**(模板级开关 require_face_photo,禁硬编码);游乐(entitlement_type='amusement')**默认开**=种子回填。
--   保留期:票失效后短期自清(valid_to+30 天),生物识别敏感信息 PIPL,越短越安全。
--   存储:复用 common/crypto/PiiCipher(AES-GCM)+ KeyVault 加密存库;访问限核销/运营角色 + 每次查看留审计痕(配套代码)。

-- ① 权益模板加开关:require_face_photo(购买/发放时是否要求拍脸照存档)
ALTER TABLE entitlement_templates
    ADD COLUMN IF NOT EXISTS require_face_photo BOOLEAN NOT NULL DEFAULT FALSE;

-- ② 已发权益加同名列(发放时从模板复制,供发放捕获判定与核销页判断该票是否有脸照)
ALTER TABLE issued_entitlements
    ADD COLUMN IF NOT EXISTS require_face_photo BOOLEAN;

-- ③ 游乐默认开:回填 entitlement_type='amusement' 的模板 require_face_photo=TRUE(种子回填,非硬编码)。
--    其余票种保持 FALSE(建表默认);运营后台可后续在模板逐个开关。
UPDATE entitlement_templates
   SET require_face_photo = TRUE
 WHERE entitlement_type = 'amusement';

-- ④ 人脸照存档表:一权益一照(entitlement_id 唯一)。photo_ciphertext=PiiCipher 密文(AES-GCM 信封串的字节)。
--    purge_after=票 valid_to+30 天,清理 job 按此硬删。captured_at=购买捕获时刻。
CREATE TABLE IF NOT EXISTS ticket_face_photos (
    id                VARCHAR(64)  PRIMARY KEY,
    entitlement_id    VARCHAR(64)  NOT NULL UNIQUE,   -- 一权益一照,重发放 ON CONFLICT 覆盖
    photo_ciphertext  BYTEA        NOT NULL,          -- PiiCipher 加密后的信封串字节(禁明文存)
    content_type      VARCHAR(32),                    -- image/jpeg 等,展示用
    captured_at       TIMESTAMPTZ,                    -- 购买/发放时的拍照时刻
    purge_after       DATE         NOT NULL,          -- =valid_to+30 天,过此日清理 job 硬删
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 清理 job 按 purge_after 扫,加索引助过滤。
CREATE INDEX IF NOT EXISTS idx_tfp_purge_after ON ticket_face_photos (purge_after);

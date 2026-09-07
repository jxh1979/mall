-- V397 审计运行 20260902T183912Z · M009 席乙 F-05：人脸照的同意留痕 id 必须真实存在于同意台账。
--
-- BSM-1.7.3「采集证件、人脸、支付账号、精确位置等敏感信息前先单独取得同意，并留同意版本、时间和范围」。
-- V395 只要求 consent_id 非空——任意非空字符串都能过库约束，权益位/暂存位都无法证明「版本、时间、范围」存在。
-- 代码侧同批：暂存、捕获、出票请求自带照片三条路径共用同一道台账闸（存在、类型 ticket_face_photo、已接受、未撤回，
-- 订单已绑客人时同意行必须属于该客人）。库上补外键：绕过服务直接 INSERT 一个台账里没有的 consent_id 也被拒。
--
-- 关于外键与删除顺序（V368 曾因「客人注销硬删会被照片行卡住」不加外键）：同意行是永久留痕、无删除路径；
-- 客人注销现为状态标记（数据物理保留）。若将来对客人做物理删除，必须先删其人脸照行（BSM-1.7.1 注销后人脸等个人信息一律删除），
-- 再级联删同意行——外键把这个顺序钉死，正是想要的行为。照片到期由清理作业硬删，不受外键影响。
--
-- 前置：两库 ticket_face_photos 现无任何行（2026-09-07 于 160 实查），无需回填。
ALTER TABLE ticket_face_photos DROP CONSTRAINT IF EXISTS fk_tfp_consent;
ALTER TABLE ticket_face_photos
    ADD CONSTRAINT fk_tfp_consent
    FOREIGN KEY (consent_id) REFERENCES customer_consents(id);

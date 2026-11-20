-- 文件版本：V0.01
-- V683：第十八批 BD 段第 361 条 361-R1（高价值存酒作废被主管复核驳回后，存酒回到作废前的状态）。
-- 依据：BSM:1072「存酒作废需照片+视频双证据+主管复核」；第十八批裁决 14「高价值的报损、盘点差异、存酒作废，必须照片＋视频＋主管复核才生效」；
-- 第十八批裁决 58「争议成立，走现有存酒作废（高价值的要照片＋视频＋主管复核，裁决 14）」。
-- 高价值作废冻结为 disputed_frozen 前记下原状态，驳回时据此恢复；取值闭集同 cancelSavedBottle 可作废的源态；研发阶段无存量（UD-07），不回填。
ALTER TABLE saved_bottles ADD COLUMN void_prior_status VARCHAR(32)
    CHECK (void_prior_status IN ('stored', 'pending_identity_review', 'disputed'));

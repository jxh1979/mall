-- 文件版本：V0.01
-- V566 第十八批第 24 条（待开发清单第 24 条 高价值判定）。
--
-- 1) 高价值判定阈值全平台统一、须同时配置上下限、调整须最高权限审批并留痕。
--    依据 ENUM:164「与后台配置的固定金额阈值比较（安全阈值，全平台统一可配，调整须最高权限审批并留痕，出厂默认 5000 元，
--    须同时配置上下限），大于或等于阈值即高价值、低于阈值即低价值」；BSM:1196 同句。
--    阈值写入只落全平台一行（project_id／merchant_id 为空），读取也只认这一行；草案进统一审批的最高权限策略，review_id 回写审批单号，
--    批准后转正、驳回即草案下线（SettlementConfigWriteService.highValueThresholdDecided）。
--    上下限与阈值同空或同非空，且 0 ≤ 下限 ≤ 阈值 ≤ 上限；出厂行 hvt_default 上下限留空（HTML 未给出厂上下限，首次调整时须一并填写）。
ALTER TABLE high_value_thresholds
    ADD COLUMN IF NOT EXISTS high_value_threshold_min_cent BIGINT,
    ADD COLUMN IF NOT EXISTS high_value_threshold_max_cent BIGINT,
    ADD COLUMN IF NOT EXISTS review_id VARCHAR(64);
ALTER TABLE high_value_thresholds
    ADD CONSTRAINT chk_hvt_threshold_bounds CHECK (
        (high_value_threshold_min_cent IS NULL) = (high_value_threshold_max_cent IS NULL)
        AND (high_value_threshold_min_cent IS NULL
             OR (0 <= high_value_threshold_min_cent AND high_value_threshold_min_cent <= high_value_threshold_cent
                 AND high_value_threshold_cent <= high_value_threshold_max_cent)));
COMMENT ON TABLE high_value_thresholds IS
    '全平台统一「高价值物品」判定阈值（分）：物品最近一次实际到货价大于或等于本值即高价值。退酒/报损/盘点差异/存酒作废共用；只一行全平台生效行，调整须最高权限审批。缺配置一律按高价值拦下。';
COMMENT ON COLUMN high_value_thresholds.high_value_threshold_cent IS '高价值判定阈值（分）：最近一次实际到货价大于或等于本值即高价值；出厂 500000=5000 元';
COMMENT ON COLUMN high_value_thresholds.high_value_threshold_min_cent IS '阈值这一参数的下限（分），与上限须同时配置';
COMMENT ON COLUMN high_value_thresholds.high_value_threshold_max_cent IS '阈值这一参数的上限（分），与下限须同时配置';
COMMENT ON COLUMN high_value_thresholds.review_id IS '本次调整的最高权限审批单号（supervisor_review_decisions.review_id）';

-- 2) 盘点差异记录人：PGM:529「申请人与直接受益人不得审批与自己相关的申请，转发不构成例外」——录差异的人不得复核自己录的差异。
ALTER TABLE inventory_variance ADD COLUMN IF NOT EXISTS recorded_by VARCHAR(64);
COMMENT ON COLUMN inventory_variance.recorded_by IS '录入本差异的账号；复核人不得是录入人';

-- 3) 高价值盘点差异照片＋视频双证据：ENUM:164「报损/盘点差异/存酒作废须照片+视频双证据+主管复核」；BSM:1183「高价值酒水报损、盘点差异、
--    存酒作废必须拍照和视频留证，并主管复核」；任务书 18D 操作员裁决补充第 14 条「只高价值的要」——低价值差异维持现状、不要求附件。
ALTER TABLE inventory_variance
    ADD COLUMN IF NOT EXISTS photo_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS video_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
COMMENT ON COLUMN inventory_variance.photo_attachment_ids IS '差异照片证据附件 id 列表；高价值差异必填（照片＋视频双证据）';
COMMENT ON COLUMN inventory_variance.video_attachment_ids IS '差异视频证据附件 id 列表；高价值差异必填（照片＋视频双证据）';

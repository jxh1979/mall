-- P1 清单#18（游乐部分）+ #21：游乐安全巡检结构化补强 + 项目入口点位列。
-- 背景与断链：
--   ① amusement_safety_inspections（V014）无 inspector_id / inspection_result / abnormal 结构化字段，
--      巡检人只能塞进 operator_note 文本，无法结构化检索/审计巡检人与异常项。
--   ② amusement_projects（V014）无入口点位列 location_point_id，项目与物理入口点位无结构化绑定
--      （容量配置 amusement_capacity_config 虽有 location_point_id，但项目主体缺该维度）。
-- 本迁移（沿用既有"目标增补字段"口径：仅 ADD COLUMN IF NOT EXISTS，不删既有 items_json/overall_result）：
--   1) amusement_projects 补 location_point_id（项目入口点位，可空；存量为 NULL 表示未绑定，向后兼容）。
--   2) amusement_safety_inspections 补 inspector_id（巡检人，可空）+ inspection_result（结构化巡检结论，可空）
--      + abnormal_found / abnormal_note（异常标记与异常说明，可空）。
--      既有 overall_result/operator_note 不动，新字段与其并存：overall_result 仍是状态机口径（pass/fail/pending_rectification），
--      inspection_result 是巡检人填报的结构化结论（与 overall_result 同枚举域，便于看板/审计直接读结构化字段）。

ALTER TABLE amusement_projects
    ADD COLUMN IF NOT EXISTS location_point_id VARCHAR(64);

ALTER TABLE amusement_safety_inspections
    ADD COLUMN IF NOT EXISTS inspector_id VARCHAR(64);

ALTER TABLE amusement_safety_inspections
    ADD COLUMN IF NOT EXISTS inspection_result VARCHAR(32);

ALTER TABLE amusement_safety_inspections
    ADD COLUMN IF NOT EXISTS abnormal_found BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE amusement_safety_inspections
    ADD COLUMN IF NOT EXISTS abnormal_note TEXT;

-- inspection_result 与 overall_result 同枚举域（可空：存量/未填报为 NULL）。
ALTER TABLE amusement_safety_inspections
    DROP CONSTRAINT IF EXISTS chk_amusement_safety_inspections_inspection_result;
ALTER TABLE amusement_safety_inspections
    ADD CONSTRAINT chk_amusement_safety_inspections_inspection_result
        CHECK (inspection_result IS NULL
               OR inspection_result IN ('pass', 'fail', 'pending_rectification'));

-- 巡检人维度检索（按项目 + 巡检人 + 巡检时间）。
CREATE INDEX IF NOT EXISTS idx_amusement_safety_inspections_inspector
    ON amusement_safety_inspections (project_id, inspector_id, inspected_at DESC);

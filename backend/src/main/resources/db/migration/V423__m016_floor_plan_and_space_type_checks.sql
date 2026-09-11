-- M016 审计 B3（2026-09-11）：三处库层闭集/契约对齐，不改数据语义、可重跑（DROP IF EXISTS 后重建）。
-- 1) 候选区域类型对齐治理地图「铺位、摊位、公共区」与 V184 字典：shape_type CHECK 增加 booth / public_area（M015-013 转入）。
--    既有六值保留；应用白名单（FloorPlanService.SHAPE_TYPES）与前端闭集另批放宽。
ALTER TABLE floor_plan_shapes DROP CONSTRAINT IF EXISTS chk_floor_plan_shapes_shape_type;
ALTER TABLE floor_plan_shapes ADD CONSTRAINT chk_floor_plan_shapes_shape_type
    CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route', 'booth', 'public_area'));
ALTER TABLE floor_plan_import_candidates DROP CONSTRAINT IF EXISTS chk_floor_plan_import_candidates_shape_type;
ALTER TABLE floor_plan_import_candidates ADD CONSTRAINT chk_floor_plan_import_candidates_shape_type
    CHECK (shape_type IN ('area', 'shop', 'entrance', 'facility', 'risk_zone', 'route', 'booth', 'public_area'));

-- 2) 平面图必须带综合体项目归属（M015-002 转入；服务层已必填并校验 projects 存在）：
--    新写/更新行强制非空，存量 NULL 行容忍（NOT VALID），不发明归属值；软引用不加 FK（与 spaces.project_id 范式一致）。
ALTER TABLE mall_floor_plans DROP CONSTRAINT IF EXISTS chk_mall_floor_plans_project_id_required;
ALTER TABLE mall_floor_plans ADD CONSTRAINT chk_mall_floor_plans_project_id_required
    CHECK (project_id IS NOT NULL) NOT VALID;

-- 3) 空间类型字典 status 闭集（V184 注释 active/disabled；listActiveSpaceTypeCodes 只认 active）：
--    按 V203 范式先归一存量脏值再上 CHECK；脏值归为 disabled（此前它们已不在下拉/硬校验中，保持行为不变）。
UPDATE space_type_dictionary SET status = 'disabled' WHERE status NOT IN ('active', 'disabled');
ALTER TABLE space_type_dictionary DROP CONSTRAINT IF EXISTS chk_space_type_dictionary_status;
ALTER TABLE space_type_dictionary ADD CONSTRAINT chk_space_type_dictionary_status
    CHECK (status IN ('active', 'disabled'));

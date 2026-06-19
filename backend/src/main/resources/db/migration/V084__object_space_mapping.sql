-- 第二阶段（待确认#4 遗留续）：扩大 AdmissionGuard over_space 真实判定的对象→空间解析覆盖面。
--
-- 背景：over_space 命中判定需先把受控对象解析到所属 space，再查该 space 是否在 actor 商户
--   生效合同的 contract_spaces（V080）租赁内。此前 resolveSpaceId 仅 merchant_service_work_order
--   （V022 已有直连 space_id）可解析，其余受控 objectType 无 对象→空间 映射 → 一律保守放行，
--   over_space 实际近乎空判。
--
-- 本迁移给"实际被 @GuardedAction(objectType=...) 受控、且需 over_space"的对象补 space 解析所需的列：
--   1) bar_tables.space_id：卡座/桌台所属空间。bar_bill（objectId=bill_id）→ bar_bills.table_id
--      → bar_tables.space_id（多级解析）。bar_tables（V013）原只到 bar_id，无 space_id。
--   2) amusement_projects.space_id：游乐项目所属空间。amusement 各受控对象（inspection /
--      amusement_maintenance_record / amusement_incident / amusement_group_booking）均带 project_id
--      → amusement_projects.space_id。amusement_projects（V014）原无任何 area/store/space 列，
--      故按现状最小改动直接补 space_id（与 spaces 体系 space_id 对齐），不引入 area_id 中间层。
--
-- 软引用风格：与 V022 work_orders.space_id / amusement、bar 既有软引用一致，不强制 FK
--   （空间可跨项目/合同复用、历史数据可能缺空间归属），仅加列+注释。仅给上述受控对象补，不泛滥加列。

-- 1) 卡座/桌台所属空间（bar_bill → bar_tables.space_id）
ALTER TABLE bar_tables
    ADD COLUMN IF NOT EXISTS space_id VARCHAR(64);

COMMENT ON COLUMN bar_tables.space_id IS
    '卡座/桌台所属空间 id（软引用 spaces.id，不强制 FK）。驱动 AdmissionGuard over_space：bar_bill→bar_bills.table_id→bar_tables.space_id。';

-- 2) 游乐项目所属空间（amusement 受控对象经 project_id → amusement_projects.space_id）
ALTER TABLE amusement_projects
    ADD COLUMN IF NOT EXISTS space_id VARCHAR(64);

COMMENT ON COLUMN amusement_projects.space_id IS
    '游乐项目所属空间 id（软引用 spaces.id，不强制 FK）。驱动 AdmissionGuard over_space：inspection/amusement_maintenance_record/amusement_incident/amusement_group_booking 经各自 project_id → amusement_projects.space_id。';

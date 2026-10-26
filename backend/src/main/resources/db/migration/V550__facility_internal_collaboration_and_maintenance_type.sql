-- 文件版本：V0.01
-- V550（第十六批 G5：待开发清单第 35、36 条）。
-- ① 物业工单类型补「内部协作工单」：ENUM L260 / BSM L1377「报修、保洁、投诉、整改、巡检、内部协作工单（内部部门间发起与流转）、其他服务请求」。
--    工单表与「类型×优先级」策略表两处 CHECK 加 internal_collaboration；时限与验收方式照现有范式在后台策略矩阵配置，本迁移不落策略行。
-- ② 设备维保类型：ENUM L267「定期保养、例行巡检、故障维修、安全检测、部件更换」；BSM L1408「维保分定期保养、例行巡检、故障维修、安全检测、部件更换」。
--    维保完成登记时必选类型，设备台账记最近一次维保类型（每次类型另随维保完成审计留痕）；存量设备不回填。
ALTER TABLE facility_work_orders DROP CONSTRAINT chk_fwo_type;
ALTER TABLE facility_work_orders ADD CONSTRAINT chk_fwo_type CHECK (work_order_type IN
    ('repair', 'cleaning', 'complaint', 'rectification', 'inspection', 'internal_collaboration', 'other'));
ALTER TABLE facility_work_order_policy DROP CONSTRAINT chk_fwop_type;
ALTER TABLE facility_work_order_policy ADD CONSTRAINT chk_fwop_type CHECK (work_order_type IN
    ('repair', 'cleaning', 'complaint', 'rectification', 'inspection', 'internal_collaboration', 'other'));

ALTER TABLE facility_equipment ADD COLUMN last_maintenance_type VARCHAR(32);
ALTER TABLE facility_equipment ADD CONSTRAINT chk_facility_equipment_last_maintenance_type CHECK (
    last_maintenance_type IS NULL
    OR last_maintenance_type IN ('scheduled_maintenance', 'routine_inspection', 'fault_repair', 'safety_test', 'part_replacement'));

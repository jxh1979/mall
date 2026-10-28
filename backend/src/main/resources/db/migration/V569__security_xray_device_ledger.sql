-- 文件版本：V0.01
-- 第 63 条（BSM:800「安检环节不产生按人记录，仅记设备状态、故障事件与维修工单留痕」；PGM:623「建设备台账，记录编号、型号、点位、责任人和状态」）：
-- 安检 X 光设备进设备台账 terminal_devices（terminal_type=security_xray，后台登记）。device_model＝设备型号（X 光登记时必填，其他设备为空）；
-- repair_work_order_id＝最近一次登记故障时开的维修工单（facility_work_orders.id）。terminal_type 照旧不加 CHECK（V111）。
ALTER TABLE terminal_devices ADD COLUMN device_model VARCHAR(128);
ALTER TABLE terminal_devices ADD COLUMN repair_work_order_id VARCHAR(64);

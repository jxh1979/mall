-- 文件版本：V0.01
-- V678（第十八批 BB 段第 318 条 318-R1）：游乐设备安全巡检的模板改为可空。
-- 依据：PGM:528「②当天没有做过合格巡检的设施不得开放」；BSM:1300「开放前巡检 + 营业中抽检 + 定期检修」；
--   PGM:846 把模板只当作排巡检计划的方式之一（「主管手动排」不需要模板）。后台游乐安全页「选择模板（可空）」不选就不传，
--   服务层（AmusementInspectionService）只在填了模板时核是否存在；此前 V014 的非空约束使不选模板的新建落 500。外键保留。
ALTER TABLE amusement_safety_inspections ALTER COLUMN template_id DROP NOT NULL;

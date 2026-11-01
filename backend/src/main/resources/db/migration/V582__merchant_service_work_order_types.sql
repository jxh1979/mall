-- 文件版本：V0.01
-- V582（第十八批 K 段第 93 条 93-R1）商户服务工单类型闭集改为报修、保洁、投诉、其他服务请求。
-- BSM:1323「我的工单（报修/保洁/投诉/其他服务请求自己发起……）」；BSM:1385「商户侧可自助提报修、保洁、投诉和其他服务请求，整改与巡检类工单由平台发起」；
-- 裁决 D-12（不含整改、含保洁）、D-13「商户专区自助提的单走 ENUM 商户服务工单链」。平台发起的整改、巡检落 V166 物业工单。
-- 码：repair（报修）、cleaning（保洁，与物业工单同码）、complaint（投诉）、service（其他服务请求）。工单表与处理时限表（V577）两处 CHECK 同改；不处理存量（UD-07）。
ALTER TABLE merchant_service_work_orders DROP CONSTRAINT chk_merchant_service_work_order_type;
ALTER TABLE merchant_service_work_orders ADD CONSTRAINT chk_merchant_service_work_order_type CHECK (work_order_type IN
    ('repair', 'cleaning', 'complaint', 'service'));
ALTER TABLE merchant_service_work_order_policies DROP CONSTRAINT chk_merchant_service_work_order_policy_type;
ALTER TABLE merchant_service_work_order_policies ADD CONSTRAINT chk_merchant_service_work_order_policy_type CHECK (work_order_type IN
    ('repair', 'cleaning', 'complaint', 'service'));

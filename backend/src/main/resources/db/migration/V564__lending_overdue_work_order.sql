-- 文件版本：V0.01
-- 第十八批第 18 条（BSM:1201「超过约定归还时间仍未归还的，系统自动生成一张工单并通知借出方主管，按与其他工单相同的超时升级路径继续催办，
-- 直到归还或双方另行处理为止」；BSM:1372「都进统一工单」）：借用单记已生成的逾期工单（同单只建一次；全部归还或实物处置完成后结束该工单），
-- 不对外返回；物业工单来源闭集加 lending_overdue（借用逾期，系统自动生成）。
ALTER TABLE inventory_transfer_orders ADD COLUMN overdue_work_order_id VARCHAR(64);
ALTER TABLE facility_work_orders DROP CONSTRAINT IF EXISTS chk_fwo_source;
ALTER TABLE facility_work_orders ADD CONSTRAINT chk_fwo_source
    CHECK (source IN ('merchant_self', 'platform', 'equipment_alarm', 'inspection', 'customer', 'lending_overdue'));

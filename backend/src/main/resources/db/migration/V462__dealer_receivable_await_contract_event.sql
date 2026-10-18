-- 文件版本：V0.01
-- 经销合同按签收等事件起算尾款时，事件未发生则应收保留、到期日待定；其他业务仍必须有到期日。
ALTER TABLE ar_ap_items ALTER COLUMN due_date DROP NOT NULL;
ALTER TABLE ar_ap_items
    ADD CONSTRAINT chk_ar_ap_due_date_contract_event
        CHECK (due_date IS NOT NULL OR source_type = 'dealer_order');

-- V273: 修复物业费出账 500 + 退款 permission denied（真实用户流程审计 2026-07-08 发现）。
--
-- 背景1（物业费出账必 500）：FinanceService.persistDerivedArApItem 派生 AR/AP 台账项时，
--   物业费的 source_id = "property_fee_" + projectId|period|merchantId（约 102 字符），
--   而 ar_ap_items.source_id / ar_ap_item_sources.source_id 为 varchar(64) → value too long → 500。
--   同类系统性隐患：id/counterparty_id 由长业务键拼接。统一放宽到 128。
-- 背景2（退款 permission denied for ar_ap_payments）：退款走 SELECT ... FOR UPDATE 需要 UPDATE 权限，
--   但受限运行时角色 complex_app 在 ar_ap_payments 上仅有 INSERT/SELECT（迁移遗漏 UPDATE/DELETE 授权）。

ALTER TABLE ar_ap_items          ALTER COLUMN id              TYPE varchar(128);
ALTER TABLE ar_ap_items          ALTER COLUMN source_id       TYPE varchar(128);
ALTER TABLE ar_ap_items          ALTER COLUMN counterparty_id TYPE varchar(128);

ALTER TABLE ar_ap_item_sources   ALTER COLUMN id            TYPE varchar(128);
ALTER TABLE ar_ap_item_sources   ALTER COLUMN ar_ap_item_id TYPE varchar(128);
ALTER TABLE ar_ap_item_sources   ALTER COLUMN source_id     TYPE varchar(128);

GRANT UPDATE, DELETE ON ar_ap_payments TO complex_app;

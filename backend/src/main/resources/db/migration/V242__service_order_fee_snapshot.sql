-- 服务订单域资金安全(2026-07-05 审计 P2-#3):分账费率取下单快照,非完成时活目录。
-- 下单时把管理费模式/值一并快照进 service_orders;完成分账用订单快照(改目录不影响在途单)。
-- 存量在途单快照列为 NULL → 完成时回退活目录(向后兼容,不阻断历史单)。
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS snap_mgmt_fee_mode  VARCHAR(16);
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS snap_mgmt_fee_value BIGINT;

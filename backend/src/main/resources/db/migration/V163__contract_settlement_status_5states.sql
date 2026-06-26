-- V163 退租结算状态 3→5 态（对齐业务 HTML §十「退租结算状态」权威：待结算/保证金抵扣中/欠费追缴中/已结清/有争议）。
-- 旧 V162 为 in_progress/disputed/settled（3 态）；本迁移扩为 5 态并改默认。dev 存量 0 行，改 CHECK 无违例。
-- 映射：待结算=pending_settlement、保证金抵扣中=deposit_offsetting、欠费追缴中=arrears_collecting、已结清=settled、有争议=disputed。

ALTER TABLE contract_settlements DROP CONSTRAINT IF EXISTS chk_contract_settlements_status;
ALTER TABLE contract_settlements ALTER COLUMN status SET DEFAULT 'pending_settlement';
ALTER TABLE contract_settlements ADD CONSTRAINT chk_contract_settlements_status
    CHECK (status IN ('pending_settlement', 'deposit_offsetting', 'arrears_collecting', 'settled', 'disputed'));

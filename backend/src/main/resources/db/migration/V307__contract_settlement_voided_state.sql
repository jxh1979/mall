-- 建设U：合同手动结算改走落账口径 + 退押金确认闸后，新增「作废」态。
-- 悬挂单处置：手动开账后被自动审批（或另一手动单）抢先落账并终止合同 → 该悬挂单在「确认退押金」时
-- 复查合同生命周期/已落账单，命中即作废（voided），绝不重复退押金/违约金。V163/V171 的 5 态扩为 6 态。
-- 存量行不受影响（旧态保留在 CHECK 中）。
ALTER TABLE contract_settlements DROP CONSTRAINT IF EXISTS chk_contract_settlements_status;
ALTER TABLE contract_settlements ADD CONSTRAINT chk_contract_settlements_status
    CHECK (status IN ('pending_settlement', 'deposit_offsetting', 'arrears_collecting', 'settled', 'disputed', 'voided'));

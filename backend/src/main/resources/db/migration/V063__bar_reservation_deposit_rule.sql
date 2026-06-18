-- R3 修复 A4（P1）：订台定金规则字段（REQ-26）。
-- 背景：R3 确认 bar_reservations 有 deposit_amount_cent/deposit_status，但矩阵声称的 deposit_rule（定金金额/比例/退没收规则）列缺。
ALTER TABLE bar_reservations
    ADD COLUMN IF NOT EXISTS deposit_rule VARCHAR(255);

-- V163 合同到期/退租/续约（② 履约闭环 S3，PRD phase6）：补合同终态列 + lifecycle CHECK 扩 expired/terminated。
-- contracts 补 terminated_at/termination_reason（退租留痕）+ renewed_from_contract_id（续约链）。均可空、纯加列。
-- V161 的 chk_contracts_lifecycle_status 取值集扩 expired（到期）/terminated（退租）；先 drop 再 re-add（PG 不支持改 CHECK）。

ALTER TABLE contracts ADD COLUMN IF NOT EXISTS terminated_at TIMESTAMPTZ;
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS termination_reason VARCHAR(500);
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS renewed_from_contract_id VARCHAR(64);

ALTER TABLE contracts DROP CONSTRAINT IF EXISTS chk_contracts_lifecycle_status;
ALTER TABLE contracts
    ADD CONSTRAINT chk_contracts_lifecycle_status
    CHECK (lifecycle_status IN ('draft', 'approved', 'rejected', 'returned', 'effective', 'expired', 'terminated'));

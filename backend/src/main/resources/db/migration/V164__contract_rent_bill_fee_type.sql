-- V164 合同账单加 fee_type（区分 租金/物业费/能源费），唯一键改 (合同,期,fee_type)，支持一期多费目挂账。
-- 旧 V162 唯一键 (contract_id, period)；本迁移换成含 fee_type。dev 存量 0 行，安全。

ALTER TABLE contract_rent_bills ADD COLUMN IF NOT EXISTS fee_type VARCHAR(24) NOT NULL DEFAULT 'rent';
ALTER TABLE contract_rent_bills DROP CONSTRAINT IF EXISTS uq_contract_rent_bills_contract_period;
ALTER TABLE contract_rent_bills DROP CONSTRAINT IF EXISTS chk_crb_fee_type;
ALTER TABLE contract_rent_bills ADD CONSTRAINT chk_crb_fee_type
    CHECK (fee_type IN ('rent', 'property_fee', 'energy_fee'));
CREATE UNIQUE INDEX IF NOT EXISTS uq_crb_contract_period_fee
    ON contract_rent_bills (contract_id, period, fee_type);

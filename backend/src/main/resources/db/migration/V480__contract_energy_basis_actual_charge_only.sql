-- 文件版本：V0.01
-- DEC-R6-28：只允许无代收或独表实际费用；不改写历史合同或账单。
-- 若仍有旧依据合同，迁移失败回滚，须先核实来源，不能自动改判。
ALTER TABLE contracts DROP CONSTRAINT chk_contracts_energy_fee_basis;
ALTER TABLE contracts ADD CONSTRAINT chk_contracts_energy_fee_basis
    CHECK (energy_fee_basis IN ('none', 'by_contract'));

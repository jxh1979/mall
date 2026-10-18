-- 文件版本：V0.01
-- 合同账单锁定实际采用的抽成规则；固定租金及其他费目不引用规则。
ALTER TABLE contract_rent_bills
    ADD COLUMN turnover_rule_id VARCHAR(64) REFERENCES turnover_rent_rules(id);

COMMENT ON COLUMN contract_rent_bills.turnover_rule_id IS '生成账单时采用的抽成规则版本，固定租金及其他费目为空';

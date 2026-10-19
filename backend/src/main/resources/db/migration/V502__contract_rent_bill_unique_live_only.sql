-- 文件版本：V0.01
-- 第五轮裁决第 2 条「租金调整生效时，作废自生效月起尚未收款的预排租金应收（留痕），按调整后月租重排剩余租期」：租金调整不开子合同，
-- 重排账单与被作废账单同属一份合同同一账期；账单唯一键 (合同,期,费目) 改为只约束未作废账单（作废行 V501 留痕保留、不删）。
DROP INDEX IF EXISTS uq_crb_contract_period_fee;
CREATE UNIQUE INDEX uq_crb_contract_period_fee
    ON contract_rent_bills (contract_id, period, fee_type) WHERE voided_at IS NULL;

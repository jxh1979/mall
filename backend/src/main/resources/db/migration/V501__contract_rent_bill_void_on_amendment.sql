-- 文件版本：V0.01
-- 第四轮裁决第 2 条「变更生效时重排」：面积/位置变更子合同生效时，作废父合同（及变更链上的前序合同）自变更生效月起尚未收款的预排租金/物业费应收（留痕），
-- 按子合同新月租和重新计期的递增重排剩余租期。依据 BSM L1353「不将未来应收视为到账，也不覆盖原收款历史」：应收台账侧沿用 V344 红冲（原行不动、追加严格取反的冲正行），
-- 账单行只记作废时刻与原因（含变更 id），账单查重（rentBillExists）不再把作废行当已出账。
ALTER TABLE contract_rent_bills
    ADD COLUMN IF NOT EXISTS voided_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS void_reason VARCHAR(500);

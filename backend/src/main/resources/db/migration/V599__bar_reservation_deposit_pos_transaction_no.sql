-- 文件版本：V0.01
-- 第十八批 S 段第 169 条 169-R2：订台定金用 POS 收款记 POS 流水号。
-- 依据：BSM:1066「……现金（记收款人）、POS（记流水号+收款人）……」；BSM:1029「现场必须允许现金和 POS 机支付，但要进入账单、日结、员工责任链和财务对账。」。
-- 员工登记定金（pay-deposit）选 POS 时必填，宽度同 bar_bill_settlements.pos_transaction_no（V062）。
ALTER TABLE bar_reservations ADD COLUMN deposit_pos_transaction_no VARCHAR(128);
COMMENT ON COLUMN bar_reservations.deposit_pos_transaction_no IS '员工登记定金（pay-deposit）选 POS 时的 POS 流水号；其他渠道为空';

-- BI S3 活证据抓真缺口:幂等键派生的合同 id 长 52 字符,
-- 挂账单据号 item_no = 费目大写 + "-" + 合同id + "-" + 期(最长 PROPERTY_FEE-…-2026-07 = 73 字符),
-- 超原 varchar(64) → 真库激活合同排账单必 500。扩至 128(uuid 分支与确定性分支全装得下)。
ALTER TABLE ar_ap_items ALTER COLUMN item_no TYPE varchar(128);

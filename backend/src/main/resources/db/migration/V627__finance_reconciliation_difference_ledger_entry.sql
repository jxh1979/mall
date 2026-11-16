-- 文件版本：V0.01
-- V627：第十八批 AL 段第 223 条 223-R1（对账差异复核通过后按认定结果入应收应付台账，同一差异只入一次）。
-- 依据：BSM:1751「高价值差异复核通过才入账、同一差异只入账一次」；ENUM:191「应收应付台账「高价值差异复核通过才入账」按本判定执行。」
--   「由此产生损失的责任方认定另行拍板。」——不一律入账：财务提交复核时按认定结果勾选「认定结果入应收应付台账」，复核通过才按实际到账与
--   平台记录之差向原往来方记应收或应付；在途次日到账、平台登记有误改正登记等结论关闭的差异不勾选、不形成往来。
-- 入账台账按 ar_ap_items 的来源（source_type=reconciliation_difference、source_id＝差异号）反查，不另建列。研发阶段无存量（UD-07）。
ALTER TABLE finance_reconciliation_differences
    ADD COLUMN ledger_entry BOOLEAN NOT NULL DEFAULT FALSE;

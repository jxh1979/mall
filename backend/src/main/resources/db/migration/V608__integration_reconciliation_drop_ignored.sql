-- 文件版本：V0.01
-- 第十八批 Y 段第 181 条 181-R3：第三方对账批次与差异的状态约束去掉 ignored（「已忽略」）。
-- 依据：DEC-R4-2026-08-04-43「删除对账差异的「已忽略」状态——账目均为电子、实时出账，不会事后发现一堆不平的账；
--   留此终态等于承认可把查不平的钱吃掉」。研发阶段无存量（UD-07），不做数据迁移。
ALTER TABLE integration_reconciliation_differences
    DROP CONSTRAINT IF EXISTS chk_integration_recon_diffs_status;
ALTER TABLE integration_reconciliation_differences
    ADD CONSTRAINT chk_integration_recon_diffs_status
    CHECK (status IN ('open', 'needs_review', 'difference_found', 'matched', 'resolved'));

ALTER TABLE integration_reconciliation_batches
    DROP CONSTRAINT IF EXISTS chk_integration_recon_batches_status;
ALTER TABLE integration_reconciliation_batches
    ADD CONSTRAINT chk_integration_recon_batches_status
    CHECK (status IN ('open', 'difference_found', 'matched', 'resolved'));

-- 对账差异正式关闭（1.6）：补 close 元数据列，供 closeFinanceReconciliationDifference 端点落库+审计留痕。
ALTER TABLE finance_reconciliation_differences ADD COLUMN IF NOT EXISTS close_reason VARCHAR(1000);
ALTER TABLE finance_reconciliation_differences ADD COLUMN IF NOT EXISTS closed_by VARCHAR(64);
ALTER TABLE finance_reconciliation_differences ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ;

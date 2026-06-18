-- Add merchant_id to all major inventory tables for data-level tenant isolation.
-- Columns are nullable so existing rows are backward-compatible; values are
-- populated from the caller's security token at creation time (never from a
-- request parameter).

ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_items_merchant
    ON inventory_items (merchant_id)
 WHERE merchant_id IS NOT NULL;

ALTER TABLE inventory_suppliers ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_suppliers_merchant
    ON inventory_suppliers (merchant_id)
 WHERE merchant_id IS NOT NULL;

ALTER TABLE inventory_purchase_requests ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_purchase_requests_merchant
    ON inventory_purchase_requests (merchant_id)
 WHERE merchant_id IS NOT NULL;

ALTER TABLE inventory_stock_balances ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_balances_merchant
    ON inventory_stock_balances (merchant_id)
 WHERE merchant_id IS NOT NULL;

ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_movements_merchant
    ON inventory_stock_movements (merchant_id)
 WHERE merchant_id IS NOT NULL;

ALTER TABLE inventory_stock_adjustments ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_adjustments_merchant
    ON inventory_stock_adjustments (merchant_id)
 WHERE merchant_id IS NOT NULL;

ALTER TABLE inventory_stock_batches ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_batches_merchant
    ON inventory_stock_batches (merchant_id)
 WHERE merchant_id IS NOT NULL;

ALTER TABLE inventory_risk_alerts ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_risk_alerts_merchant
    ON inventory_risk_alerts (merchant_id)
 WHERE merchant_id IS NOT NULL;

ALTER TABLE inventory_supplier_reconciliations ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_inventory_supplier_reconciliations_merchant
    ON inventory_supplier_reconciliations (merchant_id)
 WHERE merchant_id IS NOT NULL;

ALTER TABLE saved_bottles ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_saved_bottles_merchant
    ON saved_bottles (merchant_id)
 WHERE merchant_id IS NOT NULL;

-- Restrict adjustment_type to the known safe enum values.
-- NOT VALID defers validation of pre-existing rows; run VALIDATE CONSTRAINT
-- once the backfill migration has completed.
ALTER TABLE inventory_stock_adjustments
    ADD CONSTRAINT chk_stock_adjustment_type
        CHECK (adjustment_type IN ('gain', 'loss', 'damage', 'waste', 'count_correction'))
        NOT VALID;

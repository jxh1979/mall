ALTER TABLE bar_bill_adjustments
    ADD COLUMN IF NOT EXISTS ar_ap_item_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_bar_bill_adjustments_ar_ap_item
    ON bar_bill_adjustments (ar_ap_item_id)
    WHERE ar_ap_item_id IS NOT NULL;
